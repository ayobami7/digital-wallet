package com.wallet.history.event;

public class WalletCreatedEvent extends WalletEvent {
    @Override
    public String getEventType() {
        return "WALLET_CREATED";
    }
}