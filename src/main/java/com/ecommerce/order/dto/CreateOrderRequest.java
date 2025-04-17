package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
    
    // Optional idempotency key for preventing duplicate orders
    private String idempotencyKey;
} 