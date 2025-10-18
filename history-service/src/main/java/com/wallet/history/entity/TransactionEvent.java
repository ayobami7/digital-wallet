package com.wallet.history.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "transaction_events")
public class TransactionEvent {
    
    @Id
    private String id;
    
    @Column(name = "wallet_id", nullable = false)
    private String walletId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;
    
    protected TransactionEvent() {}
    
    public TransactionEvent(String id, String walletId, String userId, 
                          BigDecimal amount, String eventType, 
                          String transactionId, Map<String, Object> eventData) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.amount = amount;
        this.eventType = eventType;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
        this.eventData = eventData;
    }
    
    // Getters
    public String getId() { return id; }
    public String getWalletId() { return walletId; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getEventType() { return eventType; }
    public String getTransactionId() { return transactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, Object> getEventData() { return eventData; }
}