package com.wallet.history.controller;

import com.wallet.history.dto.TransactionEventResponse;
import com.wallet.history.entity.TransactionEvent;
import com.wallet.history.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HistoryController {
    
    private final HistoryService historyService;
    
    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }
    
    @GetMapping("/wallets/{walletId}/history")
    public ResponseEntity<List<TransactionEventResponse>> getWalletHistory(
            @PathVariable String walletId) {
        
        List<TransactionEvent> events = historyService.getWalletHistory(walletId);
        List<TransactionEventResponse> responses = events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/users/{userId}/activity")
    public ResponseEntity<List<TransactionEventResponse>> getUserActivity(
            @PathVariable String userId) {
        
        List<TransactionEvent> events = historyService.getUserActivity(userId);
        List<TransactionEventResponse> responses = events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/events/type/{eventType}")
    public ResponseEntity<List<TransactionEventResponse>> getEventsByType(
            @PathVariable String eventType) {
        
        List<TransactionEvent> events = historyService.getEventsByType(eventType);
        List<TransactionEventResponse> responses = events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    private TransactionEventResponse toResponse(TransactionEvent event) {
        return new TransactionEventResponse(
            event.getId(),
            event.getWalletId(),
            event.getUserId(),
            event.getAmount(),
            event.getEventType(),
            event.getTransactionId(),
            event.getCreatedAt(),
            event.getEventData()
        );
    }
}