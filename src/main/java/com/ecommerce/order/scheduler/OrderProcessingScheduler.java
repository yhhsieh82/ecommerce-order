package com.ecommerce.order.scheduler;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduler that automatically retries processing orders in PENDING_RESERVING_STOCK state.
 * This handles the case where transient errors prevented successful stock reservation.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderProcessingScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    
    @Value("${order-service.scheduler.max-retry-minutes:60}")
    private int maxRetryMinutes;
    
    @Value("${order-service.scheduler.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${order-service.scheduler.retry-delay-seconds:30}")
    private int retryDelaySeconds;
    
    /**
     * Scheduled task that runs every minute to find and retry processing orders 
     * that have been stuck in PENDING_RESERVING_STOCK state.
     */
    @Scheduled(fixedRateString = "${order-service.scheduler.retry-rate-ms:60000}")
    public void retryPendingOrders() {
        log.info("Starting scheduled task to retry pending stock reservation orders");
        
        // Find orders in PENDING_RESERVING_STOCK state
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING_RESERVING_STOCK);
        
        if (pendingOrders.isEmpty()) {
            log.debug("No orders found in PENDING_RESERVING_STOCK state");
            return;
        }
        
        log.info("Found {} orders in PENDING_RESERVING_STOCK state to retry", pendingOrders.size());
        
        // Get cutoff time for orders that have been in pending state too long
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(maxRetryMinutes);
        
        for (Order order : pendingOrders) {
            // Skip orders that have exceeded the maximum retry time
            if (order.getUpdatedAt().isBefore(cutoffTime)) {
                log.warn("Order {} has been in PENDING_RESERVING_STOCK state for more than {} minutes. Marking as INVALID", 
                        order.getId(), maxRetryMinutes);
                
                orderService.updateOrderStatus(
                        order.getId(), 
                        OrderStatus.INVALID, 
                        "Exceeded maximum retry time for stock reservation"
                );
                continue;
            }
            
            // Skip orders that have reached the maximum attempts
            if (order.getReservationAttempts() >= maxAttempts) {
                log.warn("Order {} has reached the maximum number of reservation attempts ({}). Marking as INVALID", 
                        order.getId(), maxAttempts);
                
                orderService.updateOrderStatus(
                        order.getId(), 
                        OrderStatus.INVALID, 
                        "Maximum reservation attempts reached"
                );
                continue;
            }
            
            // Skip orders that were recently attempted to avoid hammering the inventory service
            if (order.getLastReservationAttempt() != null) {
                long secondsSinceLastAttempt = ChronoUnit.SECONDS.between(
                        order.getLastReservationAttempt(), LocalDateTime.now());
                
                if (secondsSinceLastAttempt < retryDelaySeconds) {
                    log.debug("Skipping order {} as it was attempted {} seconds ago (retry delay is {} seconds)",
                            order.getId(), secondsSinceLastAttempt, retryDelaySeconds);
                    continue;
                }
            }
            
            try {
                log.info("Retrying stock reservation for order: {} (attempt {}/{})", 
                        order.getId(), order.getReservationAttempts() + 1, maxAttempts);
                orderService.processOrder(order.getId());
            } catch (Exception e) {
                log.error("Failed to retry stock reservation for order: {}", order.getId(), e);
            }
        }
    }
} 