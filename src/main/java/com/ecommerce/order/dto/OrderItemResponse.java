package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    
    private UUID id;
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
} 