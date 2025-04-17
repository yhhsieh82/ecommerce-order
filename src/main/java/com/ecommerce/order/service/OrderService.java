package com.ecommerce.order.service;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    
    /**
     * Creates a new order with initial CREATED status.
     *
     * @param request The order creation request
     * @return The created order
     */
    OrderResponse createOrder(CreateOrderRequest request);
    
    /**
     * Process the order by attempting to reserve stock from inventory.
     * Updates order status based on the result.
     *
     * @param orderId The ID of the order to process
     * @return The updated order
     */
    OrderResponse processOrder(UUID orderId);
    
    /**
     * Gets an order by its ID.
     *
     * @param orderId The ID of the order to get
     * @return The order, if found
     */
    OrderResponse getOrder(UUID orderId);
    
    /**
     * Gets all orders for a customer.
     *
     * @param customerId The ID of the customer
     * @return The list of orders
     */
    List<OrderResponse> getOrdersByCustomer(String customerId);
    
    /**
     * Gets orders by status.
     *
     * @param status The order status to filter by
     * @return The list of orders
     */
    List<OrderResponse> getOrdersByStatus(OrderStatus status);
    
    /**
     * Updates the status of an order.
     *
     * @param orderId The ID of the order to update
     * @param status The new status
     * @param failureReason Optional reason for failure, if applicable
     * @return The updated order
     */
    OrderResponse updateOrderStatus(UUID orderId, OrderStatus status, String failureReason);
} 