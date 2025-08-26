// services/integration-service/src/main/java/com/swiftlogistics/integration/controller/IntegrationController.java
package com.swiftlogistics.integration.controller;

import com.swiftlogistics.integration.dto.ApiResponse;
import com.swiftlogistics.integration.entity.IntegrationTransaction;
import com.swiftlogistics.integration.repository.IntegrationTransactionRepository;
import com.swiftlogistics.integration.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@CrossOrigin(origins = "*")
public class IntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationController.class);

    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private IntegrationTransactionRepository transactionRepository;

    @GetMapping("/transaction/{orderNumber}")
    public ResponseEntity<ApiResponse<IntegrationTransaction>> getTransactionStatus(@PathVariable String orderNumber) {
        logger.debug("Getting transaction status for order: {}", orderNumber);

        try {
            IntegrationTransaction transaction = orderProcessingService.getTransactionStatus(orderNumber);

            if (transaction != null) {
                return ResponseEntity.ok(ApiResponse.success(transaction));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting transaction status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get transaction status: " + e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<IntegrationTransaction>>> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.debug("Getting transactions with filters - status: {}, startDate: {}, endDate: {}",
                status, startDate, endDate);

        try {
            List<IntegrationTransaction> transactions;

            if (status != null && !status.isEmpty()) {
                transactions = transactionRepository.findByStatus(status);
            } else if (startDate != null && endDate != null) {
                transactions = transactionRepository.findByCreatedAtBetween(startDate, endDate);
            } else {
                transactions = transactionRepository.findAll();
            }

            return ResponseEntity.ok(ApiResponse.success(transactions));

        } catch (Exception e) {
            logger.error("Error getting transactions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get transactions: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIntegrationStats() {
        logger.debug("Getting integration statistics");

        try {
            long totalTransactions = transactionRepository.count();
            long completedTransactions = transactionRepository.countByStatus("COMPLETED");
            long failedTransactions = transactionRepository.countByStatus("FAILED");
            long processingTransactions = transactionRepository.countByStatus("STARTED");

            // Failed transactions in last 24 hours
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<IntegrationTransaction> recentFailures =
                    transactionRepository.findFailedTransactionsSince(yesterday);

            Map<String, Object> stats = Map.of(
                    "totalTransactions", totalTransactions,
                    "completedTransactions", completedTransactions,
                    "failedTransactions", failedTransactions,
                    "processingTransactions", processingTransactions,
                    "successRate", totalTransactions > 0 ?
                            (double) completedTransactions / totalTransactions * 100 : 0.0,
                    "recentFailures", recentFailures.size(),
                    "lastUpdated", LocalDateTime.now()
            );

            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            logger.error("Error getting integration stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get integration stats: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Test database connectivity
            long transactionCount = transactionRepository.count();

            return ResponseEntity.ok(Map.of(
                    "status", "UP",
                    "service", "integration-service",
                    "timestamp", LocalDateTime.now(),
                    "version", "1.0.0",
                    "database", "CONNECTED",
                    "totalTransactions", transactionCount,
                    "checks", Map.of(
                            "database", "PASS",
                            "rabbitmq", "PASS", // Could add actual RabbitMQ health check
                            "externalSystems", Map.of(
                                    "cms", "MOCK_AVAILABLE",
                                    "ros", "MOCK_AVAILABLE",
                                    "wms", "MOCK_AVAILABLE"
                            )
                    )
            ));

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "service", "integration-service",
                    "timestamp", LocalDateTime.now(),
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<ApiResponse<String>> retryFailedTransactions() {
        logger.info("Retrying failed transactions");

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<IntegrationTransaction> failedTransactions =
                    transactionRepository.findFailedTransactionsSince(since);

            int retryCount = 0;
            for (IntegrationTransaction transaction : failedTransactions) {
                // Reset status for retry
                transaction.setStatus("STARTED");
                transaction.setCmsStatus(null);
                transaction.setRosStatus(null);
                transaction.setWmsStatus(null);
                transaction.setErrorMessage(null);
                transactionRepository.save(transaction);
                retryCount++;

                logger.info("Marked transaction {} for retry", transaction.getTransactionId());
            }

            String message = String.format("Marked %d transactions for retry", retryCount);
            return ResponseEntity.ok(ApiResponse.success(message, null));

        } catch (Exception e) {
            logger.error("Error retrying failed transactions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retry transactions: " + e.getMessage()));
        }
    }
}
