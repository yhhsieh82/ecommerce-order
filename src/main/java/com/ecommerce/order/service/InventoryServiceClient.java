package com.ecommerce.order.service;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.dto.inventory.StockReservationRequest;
import com.ecommerce.order.dto.inventory.StockReservationResponse;
import com.ecommerce.order.exception.InventoryServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${external-service.inventory.url}")
    private String inventoryServiceUrl;
    
    @Value("${external-service.inventory.timeout:3000}")
    private int timeout;
    
    /**
     * Calls the Inventory service to reserve stock for the order.
     * Uses retry and circuit breaker patterns for resilience.
     *
     * @param order The order for which to reserve stock
     * @return The response from the inventory service
     * @throws InventoryServiceException If there's an error communicating with the inventory service
     */
    @Retry(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    public StockReservationResponse reserveStock(Order order) {
        StockReservationRequest request = createReservationRequest(order);
        
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + "/reserve")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(StockReservationResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Error reserving stock for order {}: Status {}, Response: {}", 
                    order.getId(), ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            
            // Differentiate between client and server errors
            if (ex.getStatusCode().is4xxClientError()) {
                // Client errors (like 400 Bad Request) are not retryable
                throw new InventoryServiceException("Client error when calling inventory service: " + ex.getMessage(), 
                        false, ex.getStatusCode());
            } else {
                // Server errors (like 500 Internal Server Error) are retryable
                throw new InventoryServiceException("Server error when calling inventory service: " + ex.getMessage(), 
                        true, ex.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("Unexpected error reserving stock for order {}", order.getId(), ex);
            throw new InventoryServiceException("Failed to communicate with inventory service: " + ex.getMessage(), 
                    true, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Fallback method that is called when all retries are exhausted or the circuit is open.
     *
     * @param order The order for which stock reservation failed
     * @param ex The exception that triggered the fallback
     * @return A failure response with appropriate messaging
     */
    private StockReservationResponse reserveStockFallback(Order order, Exception ex) {
        log.error("All retries exhausted for reserving stock for order {}", order.getId(), ex);
        
        return StockReservationResponse.builder()
                .orderId(order.getId())
                .success(false)
                .message("Failed to reserve stock after multiple attempts: " + ex.getMessage())
                .build();
    }
    
    /**
     * Creates a reservation request from an order.
     *
     * @param order The order to create a reservation request for
     * @return The stock reservation request
     */
    private StockReservationRequest createReservationRequest(Order order) {
        List<StockReservationRequest.ReservationItem> items = order.getItems().stream()
                .map(this::mapToReservationItem)
                .collect(Collectors.toList());
        
        return StockReservationRequest.builder()
                .orderId(order.getId())
                .items(items)
                .build();
    }
    
    /**
     * Maps an order item to a reservation item.
     *
     * @param orderItem The order item to map
     * @return The reservation item
     */
    private StockReservationRequest.ReservationItem mapToReservationItem(OrderItem orderItem) {
        return StockReservationRequest.ReservationItem.builder()
                .productId(orderItem.getProductId())
                .quantity(orderItem.getQuantity())
                .build();
    }
} 