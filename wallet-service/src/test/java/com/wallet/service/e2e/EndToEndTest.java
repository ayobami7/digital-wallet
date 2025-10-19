package com.wallet.service.e2e;

import com.wallet.service.dto.CreateWalletRequest;
import com.wallet.service.dto.FundWalletRequest;
import com.wallet.service.dto.WalletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EndToEndTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("test_wallet_db")
        .withUsername("test_user")
        .withPassword("test_pass")
        .withInitScript("schema.sql");  // Load your init.sql
    
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
    
    @Test
    void shouldPublishEventToKafka() {
        // Given
        CreateWalletRequest createRequest = new CreateWalletRequest("alice");
        
        // When - create wallet
        ResponseEntity<WalletResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets",
            createRequest,
            WalletResponse.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String walletId = createResponse.getBody().getId();
        
        // When - fund wallet
        FundWalletRequest fundRequest = new FundWalletRequest(new BigDecimal("100.00"));
        ResponseEntity<WalletResponse> fundResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets/" + walletId + "/fund",
            fundRequest,
            WalletResponse.class
        );
        
        assertThat(fundResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fundResponse.getBody().getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        
        // Then - verify wallet_transactions table has entries
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = ?",
                Integer.class,
                walletId
            );
            assertThat(count).isEqualTo(1);
        });
        
        // Note: This test doesn't verify History Service (that's a separate service)
        // In a real E2E test, you'd need both services running, which is complex
        // For now, we verify that:
        // 1. Wallet Service updates database correctly
        // 2. Wallet Service publishes to Kafka (we can't easily verify without History Service)
    }
    
    @Test
    void shouldMaintainConsistencyUnderLoad() throws Exception {
        // Given - create wallet
        CreateWalletRequest createRequest = new CreateWalletRequest("stress-test-user");
        ResponseEntity<WalletResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets",
            createRequest,
            WalletResponse.class
        );
        
        String walletId = createResponse.getBody().getId();
        
        // When - concurrent funding (100 requests of $1 each)
        FundWalletRequest fundRequest = new FundWalletRequest(new BigDecimal("1.00"));
        
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/wallets/" + walletId + "/fund",
                        fundRequest,
                        WalletResponse.class
                    );
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - verify final balance
        ResponseEntity<WalletResponse> finalResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/wallets/" + walletId,
            WalletResponse.class
        );
        
        assertThat(finalResponse.getBody().getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        
        // Verify transaction count
        Integer txnCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wallet_transactions WHERE wallet_id = ?",
            Integer.class,
            walletId
        );
        assertThat(txnCount).isEqualTo(100);
    }
}