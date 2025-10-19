package com.wallet.service.performance;

import com.wallet.service.dto.CreateWalletRequest;
import com.wallet.service.dto.FundWalletRequest;
import com.wallet.service.dto.WalletResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Disabled("Run manually for performance testing")
class PerformanceTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
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
    
    @Test
    void measureWalletCreationThroughput() throws Exception {
        int numRequests = 1000;
        int concurrency = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(numRequests);
        
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        
        Instant start = Instant.now();
        
        for (int i = 0; i < numRequests; i++) {
            int userId = i;
            executor.submit(() -> {
                try {
                    Instant requestStart = Instant.now();
                    
                    CreateWalletRequest request = new CreateWalletRequest("user-" + userId);
                    restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/wallets",
                        request,
                        WalletResponse.class
                    );
                    
                    long duration = Duration.between(requestStart, Instant.now()).toMillis();
                    responseTimes.add(duration);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        Instant end = Instant.now();
        long totalDuration = Duration.between(start, end).toMillis();
        
        // Calculate statistics
        double throughput = (numRequests * 1000.0) / totalDuration;
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        // Calculate percentiles
        List<Long> sorted = new ArrayList<>(responseTimes);
        sorted.sort(Long::compareTo);
        long p50 = sorted.get(sorted.size() / 2);
        long p95 = sorted.get((int) (sorted.size() * 0.95));
        long p99 = sorted.get((int) (sorted.size() * 0.99));
        
        // Print results
        System.out.println("\n=== Performance Test Results ===");
        System.out.println("Total requests: " + numRequests);
        System.out.println("Concurrency: " + concurrency);
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println("\nResponse Times:");
        System.out.println("  Average: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  Min: " + minResponseTime + "ms");
        System.out.println("  Max: " + maxResponseTime + "ms");
        System.out.println("  P50: " + p50 + "ms");
        System.out.println("  P95: " + p95 + "ms");
        System.out.println("  P99: " + p99 + "ms");
        System.out.println("================================\n");
    }
    
    @Test
    void measureFundingThroughput() throws Exception {
        // Create wallet first
        CreateWalletRequest createRequest = new CreateWalletRequest("perf-test-user");
        ResponseEntity<WalletResponse> createResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/wallets",
            createRequest,
            WalletResponse.class
        );
        
        String walletId = createResponse.getBody().getId();
        
        // Now test funding
        int numRequests = 100;
        int concurrency = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(numRequests);
        
        Instant start = Instant.now();
        
        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                try {
                    FundWalletRequest request = new FundWalletRequest(new BigDecimal("1.00"));
                    restTemplate.postForEntity(
                        "http://localhost:" + port + "/api/wallets/" + walletId + "/fund",
                        request,
                        WalletResponse.class
                    );
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        Instant end = Instant.now();
        long totalDuration = Duration.between(start, end).toMillis();
        double throughput = (numRequests * 1000.0) / totalDuration;
        
        System.out.println("\n=== Funding Performance ===");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println("Duration: " + totalDuration + "ms");
        
        // Verify final balance
        ResponseEntity<WalletResponse> finalResponse = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/wallets/" + walletId,
            WalletResponse.class
        );
        
        System.out.println("Final balance: $" + finalResponse.getBody().getBalance());
        System.out.println("Expected: $" + numRequests + ".00");
        System.out.println("==========================\n");
    }
}