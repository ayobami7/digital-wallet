package com.wallet.history.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class TransactionEventResponse {
    
    private String id;
    private String walletId;
    private String userId;
    private BigDecimal amount;
    private String eventType;
    private String transactionId;
    private Instant createdAt;
    private Map<String, Object> eventData;
    
    public TransactionEventResponse() {}
    
    public TransactionEventResponse(String id, String walletId, String userId,
                                   BigDecimal amount, String eventType,
                                   String transactionId, Instant createdAt,
                                   Map<String, Object> eventData) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.eventType = eventType;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.eventData = eventData;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Map<String, Object> getEventData() { return eventData; }
    public void setEventData(Map<String, Object> eventData) { this.eventData = eventData; }
}