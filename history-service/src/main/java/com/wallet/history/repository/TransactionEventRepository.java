package com.wallet.history.repository;

import com.wallet.history.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, String> {
    
    // Find all events for a wallet
    List<TransactionEvent> findByWalletIdOrderByCreatedAtDesc(String walletId);
    
    // Find all events for a user
    List<TransactionEvent> findByUserIdOrderByCreatedAtDesc(String userId);
    
    // Check if transaction already processed (idempotency)
    boolean existsByTransactionId(String transactionId);
    
    // Find events by type
    List<TransactionEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
}