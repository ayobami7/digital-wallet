package com.wallet.service.event;


import java.math.BigDecimal;

public class WalletFundedEvent extends WalletEvent {
    private BigDecimal amount;
    private String transactionId;

    // Getters and setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @Override
    public String getEventType() {
        return "WALLET_FUNDED";
    }
}