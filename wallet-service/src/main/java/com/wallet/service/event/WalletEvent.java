package com.wallet.service.event; 

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = WalletCreatedEvent.class, name = "WALLET_CREATED"),
    @JsonSubTypes.Type(value = WalletFundedEvent.class, name = "WALLET_FUNDED"),
    @JsonSubTypes.Type(value = TransferCompletedEvent.class, name = "TRANSFER_COMPLETED")
})
public abstract class WalletEvent {
    private String eventId;
    private String walletId;
    private String userId;
    private Instant timestamp;

    public WalletEvent() {
        this.timestamp = Instant.now();
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public abstract String getEventType();
}