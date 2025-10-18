package com.wallet.history.kafka;

import com.wallet.history.entity.TransactionEvent;
import com.wallet.history.event.TransferCompletedEvent;
import com.wallet.history.event.WalletCreatedEvent;
import com.wallet.history.event.WalletEvent;
import com.wallet.history.event.WalletFundedEvent;
import com.wallet.history.repository.TransactionEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WalletEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(WalletEventConsumer.class);
    
    private final TransactionEventRepository eventRepository;
    
    public WalletEventConsumer(TransactionEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    @KafkaListener(
        topics = "${history.kafka.topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeWalletEvent(WalletEvent event, Acknowledgment acknowledgment) {
        log.info("Received event: {} for wallet: {}", event.getEventType(), event.getWalletId());
        
        try {
            // Process event based on type
            if (event instanceof WalletCreatedEvent) {
                processWalletCreated((WalletCreatedEvent) event);
            } else if (event instanceof WalletFundedEvent) {
                processWalletFunded((WalletFundedEvent) event);
            } else if (event instanceof TransferCompletedEvent) {
                processTransferCompleted((TransferCompletedEvent) event);
            } else {
                log.warn("Unknown event type: {}", event.getClass().getName());
            }
            
            // Manually commit offset after successful processing
            acknowledgment.acknowledge();
            log.info("Event processed and committed: {}", event.getEventType());
            
        } catch (Exception e) {
            log.error("Error processing event: {}", event.getEventType(), e);
            // Don't acknowledge - message will be reprocessed
            throw e;
        }
    }
    
    private void processWalletCreated(WalletCreatedEvent event) {
        log.debug("Processing WALLET_CREATED for wallet: {}", event.getWalletId());
        
        // Check for duplicate (idempotency)
        if (eventRepository.existsByTransactionId(event.getEventId())) {
            log.warn("Event already processed, skipping: {}", event.getEventId());
            return;
        }
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("originalTimestamp", event.getTimestamp().toString());
        
        TransactionEvent transactionEvent = new TransactionEvent(
            UUID.randomUUID().toString(),
            event.getWalletId(),
            event.getUserId(),
            BigDecimal.ZERO,  // No amount for creation
            "WALLET_CREATED",
            event.getEventId(),  // Use eventId as transactionId for created events
            eventData
        );
        
        eventRepository.save(transactionEvent);
        log.info("Saved WALLET_CREATED event for wallet: {}", event.getWalletId());
    }
    
    private void processWalletFunded(WalletFundedEvent event) {
        log.debug("Processing WALLET_FUNDED for wallet: {}", event.getWalletId());
        
        // Check for duplicate
        if (eventRepository.existsByTransactionId(event.getTransactionId())) {
            log.warn("Event already processed, skipping: {}", event.getTransactionId());
            return;
        }
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("originalTimestamp", event.getTimestamp().toString());
        eventData.put("amount", event.getAmount().toString());
        
        TransactionEvent transactionEvent = new TransactionEvent(
            UUID.randomUUID().toString(),
            event.getWalletId(),
            event.getUserId(),
            event.getAmount(),
            "WALLET_FUNDED",
            event.getTransactionId(),
            eventData
        );
        
        eventRepository.save(transactionEvent);
        log.info("Saved WALLET_FUNDED event for wallet: {}, amount: {}", 
            event.getWalletId(), event.getAmount());
    }
    
    private void processTransferCompleted(TransferCompletedEvent event) {
        log.debug("Processing TRANSFER_COMPLETED from: {} to: {}", 
            event.getFromWalletId(), event.getToWalletId());
        
        // Check for duplicate
        if (eventRepository.existsByTransactionId(event.getTransactionId())) {
            log.warn("Event already processed, skipping: {}", event.getTransactionId());
            return;
        }
        
        // Create TWO events: one for sender, one for receiver
        
        // Event for sender (outgoing transfer)
        Map<String, Object> outgoingData = new HashMap<>();
        outgoingData.put("originalTimestamp", event.getTimestamp().toString());
        outgoingData.put("fromWalletId", event.getFromWalletId());
        outgoingData.put("toWalletId", event.getToWalletId());
        outgoingData.put("direction", "OUT");
        
        TransactionEvent outgoingEvent = new TransactionEvent(
            UUID.randomUUID().toString(),
            event.getFromWalletId(),  // From wallet's perspective
            event.getUserId(),
            event.getAmount().negate(),  // Negative amount (money left)
            "TRANSFER_COMPLETED",
            event.getTransactionId(),
            outgoingData
        );
        
        // Event for receiver (incoming transfer)
        Map<String, Object> incomingData = new HashMap<>();
        incomingData.put("originalTimestamp", event.getTimestamp().toString());
        incomingData.put("fromWalletId", event.getFromWalletId());
        incomingData.put("toWalletId", event.getToWalletId());
        incomingData.put("direction", "IN");
        
        // We need to get the receiver's userId (in real system, might be in event)
        // For now, we'll use a placeholder or the same userId
        TransactionEvent incomingEvent = new TransactionEvent(
            UUID.randomUUID().toString(),
            event.getToWalletId(),  // To wallet's perspective
            event.getUserId(),  // In production, fetch actual user
            event.getAmount(),  // Positive amount (money received)
            "TRANSFER_COMPLETED",
            event.getTransactionId() + "-IN",  // Different ID to allow both events
            incomingData
        );
        
        eventRepository.save(outgoingEvent);
        eventRepository.save(incomingEvent);
        
        log.info("Saved TRANSFER_COMPLETED events: {} -> {}, amount: {}", 
            event.getFromWalletId(), event.getToWalletId(), event.getAmount());
    }
}