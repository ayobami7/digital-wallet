package com.wallet.service.controller;

import com.wallet.service.dto.*;
import com.wallet.service.entity.Wallet;
import com.wallet.service.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class WalletController {
    
    private final WalletService walletService;
    
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @PostMapping("/wallets")
    public ResponseEntity<WalletResponse> createWallet(
            @Valid @RequestBody CreateWalletRequest request) {
        
        Wallet wallet = walletService.createWallet(request.getUserId());
        WalletResponse response = toWalletResponse(wallet);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/wallets/{walletId}/fund")
    public ResponseEntity<WalletResponse> fundWallet(
            @PathVariable String walletId,
            @Valid @RequestBody FundWalletRequest request) {
        
        Wallet wallet = walletService.fundWallet(walletId, request.getAmount());
        WalletResponse response = toWalletResponse(wallet);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/wallets/{walletId}/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable String walletId,
            @Valid @RequestBody TransferRequest request) {
        
        walletService.transferFunds(walletId, request.getToWalletId(), request.getAmount());
        
        TransferResponse response = new TransferResponse(
            null,  // We could track this if needed
            walletId,
            request.getToWalletId(),
            "Transfer completed successfully"
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable String walletId) {
        Wallet wallet = walletService.getWallet(walletId);
        WalletResponse response = toWalletResponse(wallet);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/{userId}/wallets")
    public ResponseEntity<List<WalletResponse>> getUserWallets(@PathVariable String userId) {
        List<Wallet> wallets = walletService.getUserWallets(userId);
        List<WalletResponse> responses = wallets.stream()
            .map(this::toWalletResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    // Helper method to convert entity to DTO
    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
            wallet.getId(),
            wallet.getUserId(),
            wallet.getBalance(),
            wallet.getCreatedAt(),
            wallet.getUpdatedAt()
        );
    }
}