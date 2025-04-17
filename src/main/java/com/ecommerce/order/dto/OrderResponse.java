package com.ecommerce.order.dto;

import com.ecommerce.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    
    private UUID id;
    private String customerId;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private String failureReason;
    
    // Reservation attempt information
    private int reservationAttempts;
    private LocalDateTime lastReservationAttempt;
} 