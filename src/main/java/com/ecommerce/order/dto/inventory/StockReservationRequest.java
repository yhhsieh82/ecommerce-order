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
public class StockReservationRequest {
    
    private UUID orderId;
    private List<ReservationItem> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationItem {
        private String productId;
        private int quantity;
    }
} 