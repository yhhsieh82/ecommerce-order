package com.ecommerce.order.controller;

import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to create order for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        
        // After creating the order, initiate processing to reserve stock
        orderService.processOrder(response.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        log.info("Received request to get order with ID: {}", orderId);
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId) {
        log.info("Received request to get orders for customer: {}", customerId);
        List<OrderResponse> response = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        log.info("Received request to get orders with status: {}", status);
        List<OrderResponse> response = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{orderId}/process")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable UUID orderId) {
        log.info("Received request to process order with ID: {}", orderId);
        OrderResponse response = orderService.processOrder(orderId);
        return ResponseEntity.ok(response);
    }
} 