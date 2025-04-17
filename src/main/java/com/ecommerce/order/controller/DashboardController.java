package com.ecommerce.order.controller;

import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for dashboard and monitoring endpoints
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final OrderService orderService;
    
    /**
     * Get a summary of orders by status
     *
     * @return A map of order counts by status
     */
    @GetMapping("/order-summary")
    public OrderSummary getOrderSummary() {
        Map<OrderStatus, Integer> countByStatus = new EnumMap<>(OrderStatus.class);
        
        // Initialize all statuses with 0 count
        for (OrderStatus status : OrderStatus.values()) {
            countByStatus.put(status, 0);
        }
        
        // Count orders by status
        for (OrderStatus status : OrderStatus.values()) {
            List<OrderResponse> orders = orderService.getOrdersByStatus(status);
            countByStatus.put(status, orders.size());
        }
        
        return new OrderSummary(countByStatus);
    }
    
    /**
     * Get orders in pending reservation status for monitoring
     *
     * @return List of orders in PENDING_RESERVING_STOCK status
     */
    @GetMapping("/pending-reservations")
    public List<OrderResponse> getPendingReservations() {
        return orderService.getOrdersByStatus(OrderStatus.PENDING_RESERVING_STOCK);
    }
    
    /**
     * Get invalid orders for troubleshooting
     *
     * @return List of orders in INVALID status
     */
    @GetMapping("/invalid-orders")
    public List<OrderResponse> getInvalidOrders() {
        return orderService.getOrdersByStatus(OrderStatus.INVALID);
    }
    
    /**
     * DTO for order summary response
     */
    @Data
    @AllArgsConstructor
    public static class OrderSummary {
        private Map<OrderStatus, Integer> orderCountByStatus;
    }
} 