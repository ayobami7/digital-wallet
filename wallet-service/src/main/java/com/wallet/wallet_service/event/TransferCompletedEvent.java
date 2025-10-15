package com.wallet.wallet_service.event;

import java.math.BigDecimal;

public class TransferCompletedEvent extends WalletEvent {
    private String fromWalletId;
    private String toWalletId;
    private BigDecimal amount;
    private String transactionId;

    // Getters and setters
    public String getFromWalletId() { return fromWalletId; }
    public void setFromWalletId(String fromWalletId) { this.fromWalletId = fromWalletId; }
    
    public String getToWalletId() { return toWalletId; }
    public void setToWalletId(String toWalletId) { this.toWalletId = toWalletId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @Override
    public String getEventType() {
        return "TRANSFER_COMPLETED";
    }
}