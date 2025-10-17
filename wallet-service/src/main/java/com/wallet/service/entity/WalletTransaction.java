package com.wallet.service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    
    @Id
    private String id;
    
    @Column(name = "wallet_id", nullable = false)
    private String walletId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    
    @Column(name = "reference_wallet_id")
    private String referenceWalletId;  // For transfers: the other wallet
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    protected WalletTransaction() {}
    
    public WalletTransaction(String id, String walletId, BigDecimal amount, 
                           TransactionType type, TransactionStatus status) {
        this.id = id;
        this.walletId = walletId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.createdAt = Instant.now();
    }
    
    // For transfers
    public WalletTransaction(String id, String walletId, BigDecimal amount, 
                           TransactionType type, TransactionStatus status,
                           String referenceWalletId) {
        this(id, walletId, amount, type, status);
        this.referenceWalletId = referenceWalletId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public String getReferenceWalletId() { return referenceWalletId; }
    public Instant getCreatedAt() { return createdAt; }
    
    // Enums
    public enum TransactionType {
        FUND,
        TRANSFER_OUT,
        TRANSFER_IN
    }
    
    public enum TransactionStatus {
        COMPLETED,
        FAILED
    }
}