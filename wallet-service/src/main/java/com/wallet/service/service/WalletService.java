package com.wallet.service.service;

import com.wallet.service.entity.Wallet;
import com.wallet.service.entity.WalletTransaction;
import com.wallet.service.entity.WalletTransaction.TransactionStatus;
import com.wallet.service.entity.WalletTransaction.TransactionType;
import com.wallet.service.event.TransferCompletedEvent;
import com.wallet.service.event.WalletCreatedEvent;
import com.wallet.service.event.WalletFundedEvent;
import com.wallet.service.kafka.WalletEventProducer;
import com.wallet.service.repository.WalletRepository;
import com.wallet.service.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {
    
    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final WalletEventProducer eventProducer;
    
    public WalletService(WalletRepository walletRepository,
                        WalletTransactionRepository transactionRepository,
                        WalletEventProducer eventProducer) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.eventProducer = eventProducer;
    }
    
    @Transactional
    public Wallet createWallet(String userId) {
        log.info("Creating wallet for user: {}", userId);
        
        String walletId = UUID.randomUUID().toString();
        Wallet wallet = new Wallet(walletId, userId);
        
        // Save to database
        wallet = walletRepository.save(wallet);
        
        // Publish event
        WalletCreatedEvent event = new WalletCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId(wallet.getId());
        event.setUserId(wallet.getUserId());
        
        eventProducer.publishEvent(event);
        
        log.info("Wallet created: {} for user: {}", walletId, userId);
        return wallet;
    }
    
    @Transactional
    public Wallet fundWallet(String walletId, BigDecimal amount) {
        log.info("Funding wallet: {} with amount: {}", walletId, amount);
        
        // Load wallet (with optimistic lock)
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        
        // Update balance
        wallet.addFunds(amount);
        
        // Create transaction record
        String transactionId = UUID.randomUUID().toString();
        WalletTransaction transaction = new WalletTransaction(
            transactionId,
            walletId,
            amount,
            TransactionType.FUND,
            TransactionStatus.COMPLETED
        );
        
        // Save both (in same transaction)
        walletRepository.save(wallet);
        transactionRepository.save(transaction);
        
        // Publish event
        WalletFundedEvent event = new WalletFundedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId(wallet.getId());
        event.setUserId(wallet.getUserId());
        event.setAmount(amount);
        event.setTransactionId(transactionId);
        
        eventProducer.publishEvent(event);
        
        log.info("Wallet funded: {} new balance: {}", walletId, wallet.getBalance());
        return wallet;
    }
    
    @Transactional
    public void transferFunds(String fromWalletId, String toWalletId, BigDecimal amount) {
        log.info("Transfer: {} -> {} amount: {}", fromWalletId, toWalletId, amount);
        
        // Validation
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        
        // Lock both wallets in consistent order (prevent deadlock!)
        List<String> walletIds = Arrays.asList(fromWalletId, toWalletId);
        Collections.sort(walletIds);  // Alphabetical order
        
        List<Wallet> wallets = walletRepository.findByIdInOrderById(walletIds);
        
        if (wallets.size() != 2) {
            throw new IllegalArgumentException("One or both wallets not found");
        }
        
        // Figure out which is which
        Wallet fromWallet = wallets.get(0).getId().equals(fromWalletId) 
            ? wallets.get(0) : wallets.get(1);
        Wallet toWallet = wallets.get(0).getId().equals(toWalletId) 
            ? wallets.get(0) : wallets.get(1);
        
        // Perform transfer
        fromWallet.deduct(amount);  // Throws if insufficient balance
        toWallet.addFunds(amount);
        
        // Create transaction records (one for each wallet)
        String transactionId = UUID.randomUUID().toString();
        
        WalletTransaction outTransaction = new WalletTransaction(
            UUID.randomUUID().toString(),
            fromWalletId,
            amount,
            TransactionType.TRANSFER_OUT,
            TransactionStatus.COMPLETED,
            toWalletId
        );
        
        WalletTransaction inTransaction = new WalletTransaction(
            UUID.randomUUID().toString(),
            toWalletId,
            amount,
            TransactionType.TRANSFER_IN,
            TransactionStatus.COMPLETED,
            fromWalletId
        );
        
        // Save everything
        walletRepository.saveAll(Arrays.asList(fromWallet, toWallet));
        transactionRepository.saveAll(Arrays.asList(outTransaction, inTransaction));
        
        // Publish event
        TransferCompletedEvent event = new TransferCompletedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId(fromWalletId);  // Primary wallet in event
        event.setUserId(fromWallet.getUserId());
        event.setFromWalletId(fromWalletId);
        event.setToWalletId(toWalletId);
        event.setAmount(amount);
        event.setTransactionId(transactionId);
        
        eventProducer.publishEvent(event);
        
        log.info("Transfer completed: {} -> {}", fromWalletId, toWalletId);
    }
    
    @Transactional(readOnly = true)
    public Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }
    
    @Transactional(readOnly = true)
    public List<Wallet> getUserWallets(String userId) {
        return walletRepository.findByUserId(userId);
    }
}