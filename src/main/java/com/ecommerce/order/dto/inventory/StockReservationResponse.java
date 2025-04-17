package com.ecommerce.order.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponse {
    
    private UUID orderId;
    private boolean success;
    private String message;
    private List<ReservationResult> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationResult {
        private String productId;
        private int quantity;
        private boolean available;
        private String message;
    }
} 