package com.wallet.service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class WalletResponse {
    private String id;
    private String userId;
    private BigDecimal balance;
    private Instant createdAt;
    private Instant updatedAt;
    
    public WalletResponse() {}
    
    public WalletResponse(String id, String userId, BigDecimal balance, 
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}