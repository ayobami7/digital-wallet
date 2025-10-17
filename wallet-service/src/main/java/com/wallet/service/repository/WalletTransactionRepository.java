package com.wallet.service.repository;

import com.wallet.service.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    
    // Find all transactions for a wallet
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(String walletId);
}