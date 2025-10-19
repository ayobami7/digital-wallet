package com.wallet.service.integration;

import com.wallet.service.dto.CreateWalletRequest;
import com.wallet.service.dto.FundWalletRequest;
import com.wallet.service.dto.TransferRequest;
import com.wallet.service.dto.WalletResponse;
import com.wallet.service.entity.Wallet;
import com.wallet.service.entity.WalletTransaction;
import com.wallet.service.repository.WalletRepository;
import com.wallet.service.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WalletServiceIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private WalletTransactionRepository transactionRepository;
    
    // Testcontainers - these start real Docker containers
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("test_wallet_db")
        .withUsername("test_user")
        .withPassword("test_pass");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    // Configure Spring to use test containers
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    
    @BeforeEach
    void setUp() {
        // Clean database before each test
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }
    
    @Test
    void shouldCreateWallet() {
        // Given
        CreateWalletRequest request = new CreateWalletRequest("alice");
        
        // When
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets",
            request,
            WalletResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("alice");
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // Verify database
        List<Wallet> wallets = walletRepository.findByUserId("alice");
        assertThat(wallets).hasSize(1);
        assertThat(wallets.get(0).getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
    
    @Test
    void shouldFundWallet() {
        // Given - create wallet first
        Wallet wallet = new Wallet("test-wallet-1", "alice");
        walletRepository.save(wallet);
        
        FundWalletRequest request = new FundWalletRequest(new BigDecimal("100.00"));
        
        // When
        ResponseEntity<WalletResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets/test-wallet-1/fund",
            request,
            WalletResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        
        // Verify database
        Wallet updated = walletRepository.findById("test-wallet-1").orElseThrow();
        assertThat(updated.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(updated.getVersion()).isEqualTo(1L); // Version incremented
        
        // Verify transaction log
        List<WalletTransaction> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc("test-wallet-1");
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(WalletTransaction.TransactionType.FUND);
        assertThat(transactions.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
    
    @Test
    void shouldRejectNegativeAmount() {
        // Given
        Wallet wallet = new Wallet("test-wallet-2", "bob");
        walletRepository.save(wallet);
        
        FundWalletRequest request = new FundWalletRequest(new BigDecimal("-50.00"));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets/test-wallet-2/fund",
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Amount must be at least 0.01");
    }
    
    @Test
    void shouldTransferFunds() {
        // Given - create two wallets
        Wallet alice = new Wallet("alice-wallet", "alice");
        alice.addFunds(new BigDecimal("100.00"));
        walletRepository.save(alice);
        
        Wallet bob = new Wallet("bob-wallet", "bob");
        walletRepository.save(bob);
        
        TransferRequest request = new TransferRequest("bob-wallet", new BigDecimal("30.00"));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets/alice-wallet/transfer",
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify balances
        Wallet aliceUpdated = walletRepository.findById("alice-wallet").orElseThrow();
        Wallet bobUpdated = walletRepository.findById("bob-wallet").orElseThrow();
        
        assertThat(aliceUpdated.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(bobUpdated.getBalance()).isEqualByComparingTo(new BigDecimal("30.00"));
        
        // Verify transactions
        List<WalletTransaction> aliceTransactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc("alice-wallet");
        List<WalletTransaction> bobTransactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc("bob-wallet");
        
        assertThat(aliceTransactions).hasSize(1);
        assertThat(aliceTransactions.get(0).getType()).isEqualTo(WalletTransaction.TransactionType.TRANSFER_OUT);
        
        assertThat(bobTransactions).hasSize(1);
        assertThat(bobTransactions.get(0).getType()).isEqualTo(WalletTransaction.TransactionType.TRANSFER_IN);
    }
    
    @Test
    void shouldRejectTransferWithInsufficientBalance() {
        // Given
        Wallet alice = new Wallet("alice-wallet-2", "alice");
        alice.addFunds(new BigDecimal("50.00"));
        walletRepository.save(alice);
        
        Wallet bob = new Wallet("bob-wallet-2", "bob");
        walletRepository.save(bob);
        
        TransferRequest request = new TransferRequest("bob-wallet-2", new BigDecimal("100.00"));
        
        // When
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets/alice-wallet-2/transfer",
            request,
            String.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Insufficient balance");
        
        // Verify balances unchanged
        Wallet aliceUnchanged = walletRepository.findById("alice-wallet-2").orElseThrow();
        assertThat(aliceUnchanged.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));
    }
    
    @Test
    void shouldHandleConcurrentFunding() throws Exception {
        // Given
        Wallet wallet = new Wallet("concurrent-wallet", "alice");
        walletRepository.save(wallet);
        
        // When - simulate concurrent requests
        FundWalletRequest request = new FundWalletRequest(new BigDecimal("10.00"));
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/wallets/concurrent-wallet/fund",
                    request,
                    WalletResponse.class
                );
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/wallets/concurrent-wallet/fund",
                    request,
                    WalletResponse.class
                );
            }
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        // Then - verify final balance is correct (no lost updates)
        Wallet finalWallet = walletRepository.findById("concurrent-wallet").orElseThrow();
        assertThat(finalWallet.getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
        // 20 requests × $10 = $200
        
        // Verify all transactions recorded
        List<WalletTransaction> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc("concurrent-wallet");
        assertThat(transactions).hasSize(20);
    }
}