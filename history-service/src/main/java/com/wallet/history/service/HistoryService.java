package com.wallet.history.service;

import com.wallet.history.entity.TransactionEvent;
import com.wallet.history.repository.TransactionEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HistoryService {
    
    private final TransactionEventRepository eventRepository;
    
    public HistoryService(TransactionEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    @Transactional(readOnly = true)
    public List<TransactionEvent> getWalletHistory(String walletId) {
        return eventRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionEvent> getUserActivity(String userId) {
        return eventRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionEvent> getEventsByType(String eventType) {
        return eventRepository.findByEventTypeOrderByCreatedAtDesc(eventType);
    }
}