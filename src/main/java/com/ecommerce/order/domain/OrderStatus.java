package com.ecommerce.order.domain;

/**
 * Represents the possible states of an order in the pre-payment flow.
 */
public enum OrderStatus {
    // Initial state when order is first created
    CREATED,
    
    // Indicates that the system is attempting to reserve stock from inventory
    PENDING_RESERVING_STOCK,
    
    // Indicates that stock has been successfully reserved and order is ready for payment
    PENDING_PAYMENT,
    
    // Indicates that the order has failed business validation or encountered an unrecoverable error
    INVALID
} 