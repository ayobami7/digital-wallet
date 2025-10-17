package com.wallet.service.dto;

public class TransferResponse {
    private String transactionId;
    private String fromWalletId;
    private String toWalletId;
    private String message;
    
    public TransferResponse() {}
    
    public TransferResponse(String transactionId, String fromWalletId, 
                          String toWalletId, String message) {
        this.transactionId = transactionId;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.message = message;
    }
    
    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public String getFromWalletId() { return fromWalletId; }
    public void setFromWalletId(String fromWalletId) { this.fromWalletId = fromWalletId; }
    
    public String getToWalletId() { return toWalletId; }
    public void setToWalletId(String toWalletId) { this.toWalletId = toWalletId; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}