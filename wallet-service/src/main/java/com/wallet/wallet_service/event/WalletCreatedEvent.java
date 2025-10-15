package com.wallet.wallet_service.event;  

public class WalletCreatedEvent extends WalletEvent {
    @Override
    public String getEventType() {
        return "WALLET_CREATED";
    }
}