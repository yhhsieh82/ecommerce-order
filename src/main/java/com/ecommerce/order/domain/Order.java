package com.ecommerce.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String customerId;
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private BigDecimal totalAmount;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    private String failureReason;
    
    // Track reservation attempts for debugging and limiting retries
    private int reservationAttempts;
    
    // Last time a reservation was attempted
    private LocalDateTime lastReservationAttempt;
    
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }
        this.reservationAttempts = 0;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    /**
     * Increments the reservation attempt counter and updates the last attempt timestamp.
     */
    public void incrementReservationAttempts() {
        this.reservationAttempts++;
        this.lastReservationAttempt = LocalDateTime.now();
    }
} 