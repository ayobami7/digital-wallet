-- Wallets table (owned by Wallet Service)
CREATE TABLE wallets (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Wallet transactions table (owned by Wallet Service)
CREATE TABLE wallet_transactions (
    id VARCHAR(36) PRIMARY KEY,
    wallet_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference_wallet_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id),
    CONSTRAINT valid_type CHECK (type IN ('FUND', 'TRANSFER_OUT', 'TRANSFER_IN')),
    CONSTRAINT valid_status CHECK (status IN ('COMPLETED', 'FAILED'))
);

CREATE INDEX idx_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_transactions_created_at ON wallet_transactions(created_at DESC);

-- Transaction events table (owned by History Service)
CREATE TABLE transaction_events (
    id VARCHAR(36) PRIMARY KEY,
    wallet_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    transaction_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    event_data JSONB,
    CONSTRAINT valid_event_type CHECK (event_type IN ('WALLET_CREATED', 'WALLET_FUNDED', 'TRANSFER_COMPLETED', 'TRANSFER_FAILED'))
);

CREATE INDEX idx_events_wallet_id ON transaction_events(wallet_id);
CREATE INDEX idx_events_user_id ON transaction_events(user_id);
CREATE INDEX idx_events_transaction_id ON transaction_events(transaction_id);
CREATE INDEX idx_events_created_at ON transaction_events(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE wallets IS 'Current wallet balances - source of truth for money';
COMMENT ON TABLE wallet_transactions IS 'Transaction log for wallet operations';
COMMENT ON TABLE transaction_events IS 'Event-sourced history built from Kafka events';
COMMENT ON COLUMN wallets.version IS 'Optimistic locking version number';