package com.ecommerce.order.service;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.inventory.StockReservationResponse;
import com.ecommerce.order.exception.InventoryServiceException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;
    
    @Value("${order-service.scheduler.max-attempts:5}")
    private int maxReservationAttempts;
    
    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating new order for customer: {}", request.getCustomerId());
        
        // Create order entity
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.CREATED)
                .build();
        
        // Add items to order
        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    // We'll need to fetch product details from a product service in a real implementation
                    .productName("Product " + itemRequest.getProductId())
                    .unitPrice(BigDecimal.TEN) // Placeholder price
                    .build();
            
            order.addItem(item);
        }
        
        // Calculate total amount
        BigDecimal totalAmount = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        order.setTotalAmount(totalAmount);
        
        // Save the order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());
        
        return mapToOrderResponse(savedOrder);
    }
    
    @Override
    @Transactional
    public OrderResponse processOrder(UUID orderId) {
        log.info("Processing order: {}", orderId);
        
        // Get the order
        Order order = getOrderEntity(orderId);
        
        // Check if max attempts reached
        if (order.getReservationAttempts() >= maxReservationAttempts) {
            log.warn("Maximum reservation attempts ({}) reached for order: {}", 
                    maxReservationAttempts, orderId);
            
            order.setStatus(OrderStatus.INVALID);
            order.setFailureReason("Maximum reservation attempts reached");
            orderRepository.save(order);
            
            return mapToOrderResponse(order);
        }
        
        // Increment reservation attempts
        order.incrementReservationAttempts();
        
        // Update status to PENDING_RESERVING_STOCK if not already
        if (order.getStatus() != OrderStatus.PENDING_RESERVING_STOCK) {
            order.setStatus(OrderStatus.PENDING_RESERVING_STOCK);
        }
        
        // Save updated attempt count and status
        orderRepository.save(order);
        
        try {
            // Call inventory service to reserve stock
            StockReservationResponse reservationResponse = inventoryServiceClient.reserveStock(order);
            
            // Check if reservation was successful
            if (reservationResponse.isSuccess()) {
                // Update order status to PENDING_PAYMENT
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                log.info("Stock reserved successfully for order: {}", orderId);
            } else {
                // Update order status to INVALID if reservation failed
                order.setStatus(OrderStatus.INVALID);
                order.setFailureReason(reservationResponse.getMessage());
                log.warn("Failed to reserve stock for order: {}. Reason: {}", 
                        orderId, reservationResponse.getMessage());
            }
            
            orderRepository.save(order);
            return mapToOrderResponse(order);
            
        } catch (InventoryServiceException ex) {
            log.error("Error reserving stock for order: {}", orderId, ex);
            
            // If the error is not retryable, mark the order as INVALID
            if (!ex.isRetryable()) {
                order.setStatus(OrderStatus.INVALID);
                order.setFailureReason("Stock reservation failed: " + ex.getMessage());
                orderRepository.save(order);
            }
            
            // For retryable errors, the order remains in PENDING_RESERVING_STOCK state
            // and will be retried later by the scheduler
            
            return mapToOrderResponse(order);
        }
    }
    
    @Override
    public OrderResponse getOrder(UUID orderId) {
        Order order = getOrderEntity(orderId);
        return mapToOrderResponse(order);
    }
    
    @Override
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status, String failureReason) {
        Order order = getOrderEntity(orderId);
        order.setStatus(status);
        
        if (failureReason != null) {
            order.setFailureReason(failureReason);
        }
        
        Order updatedOrder = orderRepository.save(order);
        log.info("Updated order {} status to {}", orderId, status);
        
        return mapToOrderResponse(updatedOrder);
    }
    
    /**
     * Maps an Order entity to an OrderResponse DTO.
     *
     * @param order The order entity to map
     * @return The order response DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .totalAmount(order.getTotalAmount())
                .items(itemResponses)
                .failureReason(order.getFailureReason())
                .reservationAttempts(order.getReservationAttempts())
                .lastReservationAttempt(order.getLastReservationAttempt())
                .build();
    }
    
    /**
     * Maps an OrderItem entity to an OrderItemResponse DTO.
     *
     * @param item The order item entity to map
     * @return The order item response DTO
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
    
    /**
     * Gets an order entity by ID.
     *
     * @param orderId The ID of the order to get
     * @return The order entity
     * @throws OrderNotFoundException if the order is not found
     */
    private Order getOrderEntity(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
    }
} 