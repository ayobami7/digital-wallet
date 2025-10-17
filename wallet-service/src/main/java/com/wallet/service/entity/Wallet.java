package com.wallet.service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {
    
    @Id
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Version  // This enables optimistic locking!
    @Column(nullable = false)
    private Long version = 0L;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // JPA requires a no-arg constructor
    protected Wallet() {}
    
    // Constructor for creating new wallets
    public Wallet(String id, String userId) {
        this.id = id;
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Business method: add funds
    public void addFunds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }
    
    // Business method: deduct funds
    public void deduct(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}