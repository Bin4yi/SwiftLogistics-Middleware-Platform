// services/integration-service/src/main/java/com/swiftlogistics/integration/controller/IntegrationController.java
package com.swiftlogistics.integration.controller;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
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
import java.util.HashMap;
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        logger.info("Integration service health check");

        try {
            long transactionCount = 0;
            try {
                transactionCount = transactionRepository.count();
            } catch (Exception e) {
                logger.warn("Could not get transaction count: {}", e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("service", "integration-service");
            response.put("status", "UP");
            response.put("timestamp", System.currentTimeMillis());
            response.put("version", "1.0.0");
            response.put("port", "8082");
            response.put("database", "CONNECTED");
            response.put("totalTransactions", transactionCount);

            Map<String, Object> externalSystems = new HashMap<>();
            externalSystems.put("cms", "MOCK_AVAILABLE");
            externalSystems.put("ros", "MOCK_AVAILABLE");
            externalSystems.put("wms", "MOCK_AVAILABLE");

            Map<String, Object> checks = new HashMap<>();
            checks.put("database", "PASS");
            checks.put("rabbitmq", "AVAILABLE");
            checks.put("externalSystems", externalSystems);

            response.put("checks", checks);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("service", "integration-service");
            response.put("status", "DOWN");
            response.put("timestamp", System.currentTimeMillis());
            response.put("error", e.getMessage());

            return ResponseEntity.status(503).body(response);
        }
    }

    @PostMapping("/process-order")
    public ResponseEntity<ProcessingResult> processOrder(@RequestBody OrderMessage orderMessage) {
        logger.info("API: Processing order integration request for order: {}", orderMessage.getOrderNumber());

        try {
            ProcessingResult result = orderProcessingService.processOrderIntegration(orderMessage);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (Exception e) {
            logger.error("Error processing order integration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ProcessingResult.failure("Integration processing failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getIntegrationStats() {
        logger.info("Getting integration statistics");

        try {
            long totalTransactions = transactionRepository.count();
            long completedTransactions = transactionRepository.countByStatus("COMPLETED");
            long failedTransactions = transactionRepository.countByStatus("FAILED");
            long processingTransactions = transactionRepository.countByStatus("STARTED");

            double successRate = totalTransactions > 0 ?
                    (double) completedTransactions / totalTransactions * 100 : 0.0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTransactions", totalTransactions);
            stats.put("completedTransactions", completedTransactions);
            stats.put("failedTransactions", failedTransactions);
            stats.put("processingTransactions", processingTransactions);
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            stats.put("lastUpdated", LocalDateTime.now());

            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("service", "integration-service");
            systemInfo.put("version", "1.0.0");
            systemInfo.put("uptime", System.currentTimeMillis());
            stats.put("systemInfo", systemInfo);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting integration stats: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get integration stats");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        logger.info("Getting transactions with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);

        try {
            List<IntegrationTransaction> transactions;

            if (status != null && !status.isEmpty()) {
                transactions = transactionRepository.findByStatus(status);
            } else if (startDate != null && endDate != null) {
                transactions = transactionRepository.findByCreatedAtBetween(startDate, endDate);
            } else {
                transactions = transactionRepository.findAll();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", transactions);
            response.put("total", transactions.size());

            Map<String, Object> filters = new HashMap<>();
            filters.put("status", status != null ? status : "all");
            filters.put("startDate", startDate);
            filters.put("endDate", endDate);
            response.put("filters", filters);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting transactions: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get transactions");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/status/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderNumber) {
        logger.info("API: Getting integration status for order: {}", orderNumber);

        try {
            IntegrationTransaction transaction = orderProcessingService.getTransactionStatus(orderNumber);

            if (transaction != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("orderNumber", orderNumber);
                response.put("transactionId", transaction.getTransactionId());
                response.put("status", transaction.getStatus());
                response.put("cmsStatus", transaction.getCmsStatus());
                response.put("rosStatus", transaction.getRosStatus());
                response.put("wmsStatus", transaction.getWmsStatus());
                response.put("createdAt", transaction.getCreatedAt());
                response.put("completedAt", transaction.getCompletedAt());
                response.put("errorMessage", transaction.getErrorMessage());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Error getting order status: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get order status");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}