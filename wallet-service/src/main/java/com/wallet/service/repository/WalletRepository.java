package com.wallet.service.repository;

import com.wallet.service.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, String> {
    
    // Find all wallets for a user
    List<Wallet> findByUserId(String userId);
    
    // Pessimistic locking for transfers (prevent deadlocks)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id IN :ids ORDER BY w.id")
    List<Wallet> findByIdInOrderById(List<String> ids);
}