package com.wallet.service.kafka;

import com.wallet.service.event.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class WalletEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(WalletEventProducer.class);
    
    private final KafkaTemplate<String, WalletEvent> kafkaTemplate;
    private final String topic;
    
    public WalletEventProducer(
            KafkaTemplate<String, WalletEvent> kafkaTemplate,
            @Value("${wallet.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    
    public void publishEvent(WalletEvent event) {
        log.info("Publishing event: {} for wallet: {}", event.getEventType(), event.getWalletId());
        
        CompletableFuture<SendResult<String, WalletEvent>> future = 
            kafkaTemplate.send(topic, event.getWalletId(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published successfully: {} to partition: {}", 
                    event.getEventType(), 
                    result.getRecordMetadata().partition());
            } else {
                log.error("Failed to publish event: {}", event.getEventType(), ex);
                // In production: retry, dead letter queue, alert
            }
        });
    }
}