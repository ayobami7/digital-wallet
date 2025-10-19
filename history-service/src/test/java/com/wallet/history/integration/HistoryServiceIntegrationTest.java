package com.wallet.history.integration;

import com.wallet.history.entity.TransactionEvent;
import com.wallet.history.event.TransferCompletedEvent;
import com.wallet.history.event.WalletCreatedEvent;
import com.wallet.history.event.WalletFundedEvent;
import com.wallet.history.repository.TransactionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class HistoryServiceIntegrationTest {
    
    @Autowired
    private TransactionEventRepository eventRepository;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("test_wallet_db")
        .withUsername("test_user")
        .withPassword("test_pass");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }
    
    @Test
    void shouldConsumeWalletCreatedEvent() {
        // Given
        WalletCreatedEvent event = new WalletCreatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId("test-wallet-1");
        event.setUserId("alice");
        event.setTimestamp(Instant.now());
        
        // When - publish to Kafka
        kafkaTemplate.send("wallet_events", event.getWalletId(), event);
        
        // Then - wait for event to be processed (async!)
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<TransactionEvent> events = eventRepository.findByWalletIdOrderByCreatedAtDesc("test-wallet-1");
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("WALLET_CREATED");
            assertThat(events.get(0).getUserId()).isEqualTo("alice");
        });
    }
    
    @Test
    void shouldConsumeWalletFundedEvent() {
        // Given
        WalletFundedEvent event = new WalletFundedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId("test-wallet-2");
        event.setUserId("bob");
        event.setAmount(new BigDecimal("100.00"));
        event.setTransactionId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());
        
        // When
        kafkaTemplate.send("wallet_events", event.getWalletId(), event);
        
        // Then
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<TransactionEvent> events = eventRepository.findByWalletIdOrderByCreatedAtDesc("test-wallet-2");
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getEventType()).isEqualTo("WALLET_FUNDED");
            assertThat(events.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        });
    }
    
    @Test
    void shouldHandleDuplicateEvents() {
        // Given
        String transactionId = UUID.randomUUID().toString();
        
        WalletFundedEvent event = new WalletFundedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId("test-wallet-3");
        event.setUserId("charlie");
        event.setAmount(new BigDecimal("50.00"));
        event.setTransactionId(transactionId);
        event.setTimestamp(Instant.now());
        
        // When - send same event twice
        kafkaTemplate.send("wallet_events", event.getWalletId(), event);
        kafkaTemplate.send("wallet_events", event.getWalletId(), event);
        
        // Then - should only create one record (idempotency)
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<TransactionEvent> events = eventRepository.findByWalletIdOrderByCreatedAtDesc("test-wallet-3");
            assertThat(events).hasSize(1);  // Only one, not two!
        });
    }
    
    @Test
    void shouldCreateTwoEventsForTransfer() {
        // Given
        TransferCompletedEvent event = new TransferCompletedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setWalletId("alice-wallet");
        event.setUserId("alice");
        event.setFromWalletId("alice-wallet");
        event.setToWalletId("bob-wallet");
        event.setAmount(new BigDecimal("30.00"));
        event.setTransactionId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());
        
        // When
        kafkaTemplate.send("wallet_events", event.getWalletId(), event);
        
        // Then - should create events for both wallets
        await().atMost(10, SECONDS).untilAsserted(() -> {
            // Alice's event (outgoing)
            List<TransactionEvent> aliceEvents = eventRepository.findByWalletIdOrderByCreatedAtDesc("alice-wallet");
            assertThat(aliceEvents).hasSize(1);
            assertThat(aliceEvents.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("-30.00"));
            
            // Bob's event (incoming)
            List<TransactionEvent> bobEvents = eventRepository.findByWalletIdOrderByCreatedAtDesc("bob-wallet");
            assertThat(bobEvents).hasSize(1);
            assertThat(bobEvents.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        });
    }
    
    @Test
    void shouldProcessEventsInOrder() {
        // Given - multiple events for same wallet
        String walletId = "test-wallet-4";
        
        WalletCreatedEvent created = new WalletCreatedEvent();
        created.setEventId(UUID.randomUUID().toString());
        created.setWalletId(walletId);
        created.setUserId("dave");
        created.setTimestamp(Instant.now());
        
        WalletFundedEvent funded1 = new WalletFundedEvent();
        funded1.setEventId(UUID.randomUUID().toString());
        funded1.setWalletId(walletId);
        funded1.setUserId("dave");
        funded1.setAmount(new BigDecimal("100.00"));
        funded1.setTransactionId(UUID.randomUUID().toString());
        funded1.setTimestamp(Instant.now().plusSeconds(1));
        
        WalletFundedEvent funded2 = new WalletFundedEvent();
        funded2.setEventId(UUID.randomUUID().toString());
        funded2.setWalletId(walletId);
        funded2.setUserId("dave");
        funded2.setAmount(new BigDecimal("50.00"));
        funded2.setTransactionId(UUID.randomUUID().toString());
        funded2.setTimestamp(Instant.now().plusSeconds(2));
        
        // When - send in sequence
        kafkaTemplate.send("wallet_events", walletId, created);
        kafkaTemplate.send("wallet_events", walletId, funded1);
        kafkaTemplate.send("wallet_events", walletId, funded2);
        
        // Then - all events processed
        await().atMost(10, SECONDS).untilAsserted(() -> {
            List<TransactionEvent> events = eventRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
            assertThat(events).hasSize(3);
            
            // Verify order (DESC, so newest first)
            assertThat(events.get(0).getEventType()).isEqualTo("WALLET_FUNDED");
            assertThat(events.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
            
            assertThat(events.get(1).getEventType()).isEqualTo("WALLET_FUNDED");
            assertThat(events.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            
            assertThat(events.get(2).getEventType()).isEqualTo("WALLET_CREATED");
        });
    }
}