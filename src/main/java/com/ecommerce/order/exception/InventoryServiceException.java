package com.ecommerce.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when there's an error communicating with the inventory service.
 */
@Getter
public class InventoryServiceException extends RuntimeException {
    
    private final boolean retryable;
    private final HttpStatusCode statusCode;
    
    public InventoryServiceException(String message, boolean retryable, HttpStatusCode statusCode) {
        super(message);
        this.retryable = retryable;
        this.statusCode = statusCode;
    }
    
    public InventoryServiceException(String message, boolean retryable, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
        this.statusCode = statusCode;
    }
} 