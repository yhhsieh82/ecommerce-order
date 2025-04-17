package com.ecommerce.order.repository;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByCustomerId(String customerId);
    
    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);
} 