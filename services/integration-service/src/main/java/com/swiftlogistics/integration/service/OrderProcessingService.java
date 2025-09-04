// services/integration-service/src/main/java/com/swiftlogistics/integration/service/OrderProcessingService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import com.swiftlogistics.integration.entity.IntegrationTransaction;
import com.swiftlogistics.integration.repository.IntegrationTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);

    @Autowired
    private CMSIntegrationService cmsIntegrationService;

    @Autowired
    private ROSIntegrationService rosIntegrationService;

    @Autowired
    private WMSIntegrationService wmsIntegrationService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IntegrationTransactionRepository transactionRepository;

    /**
     * Main entry point for processing order integration via API
     * This is the method called by IntegrationController
     */
    public ProcessingResult processOrderIntegration(OrderMessage orderMessage) {
        logger.info("Starting integration processing for order: {}", orderMessage.getOrderNumber());

        // Create transaction record for saga pattern
        IntegrationTransaction transaction = createNewTransaction(orderMessage.getOrderNumber());

        try {
            // Execute saga steps synchronously for API calls
            executeSagaSteps(transaction, orderMessage);

            logger.info("Order {} integration processing completed successfully", orderMessage.getOrderNumber());
            return ProcessingResult.success(
                    "Order integration completed successfully",
                    "Transaction ID: " + transaction.getTransactionId()
            );

        } catch (Exception e) {
            logger.error("Error processing order integration {}: {}", orderMessage.getOrderNumber(), e.getMessage(), e);
            handleSagaFailure(transaction, e.getMessage());
            return ProcessingResult.failure(
                    "Order integration failed: " + e.getMessage(),
                    "Transaction ID: " + transaction.getTransactionId()
            );
        }
    }

    /**
     * Message queue listener for async order processing
     */
    @RabbitListener(queues = "${rabbitmq.queues.order-processing:order.processing.queue}")
    public void processOrder(Map<String, Object> orderData) {
        String orderNumber = (String) orderData.get("orderNumber");
        logger.info("Processing order from queue: {}", orderNumber);

        // Create transaction record for saga pattern
        IntegrationTransaction transaction = createNewTransaction(orderNumber);

        try {
            OrderMessage orderMessage = mapToOrderMessage(orderData);

            // Execute saga steps
            executeSagaSteps(transaction, orderMessage);

        } catch (Exception e) {
            logger.error("Error processing order {}: {}", orderNumber, e.getMessage(), e);
            handleSagaFailure(transaction, e.getMessage());
        }
    }

    private IntegrationTransaction createNewTransaction(String orderNumber) {
        IntegrationTransaction transaction = new IntegrationTransaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setOrderNumber(orderNumber);
        transaction.setStatus("STARTED");
        transaction.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(transaction);
    }

    private void executeSagaSteps(IntegrationTransaction transaction, OrderMessage orderMessage) {
        try {
            // Step 1: Register with CMS
            logger.info("Step 1: Registering order {} with CMS", orderMessage.getOrderNumber());
            transaction.setCmsStatus("PROCESSING");
            transactionRepository.save(transaction);

            ProcessingResult cmsResult = cmsIntegrationService.registerOrder(orderMessage);
            if (!cmsResult.isSuccess()) {
                throw new RuntimeException("CMS registration failed: " + cmsResult.getMessage());
            }

            transaction.setCmsStatus("COMPLETED");
            transaction.setCmsResponse(cmsResult.getDetails());
            transactionRepository.save(transaction);

            // Step 2: Add to WMS
            logger.info("Step 2: Adding order {} to WMS", orderMessage.getOrderNumber());
            transaction.setWmsStatus("PROCESSING");
            transactionRepository.save(transaction);

            ProcessingResult wmsResult = wmsIntegrationService.addPackage(orderMessage);
            if (!wmsResult.isSuccess()) {
                // Compensate CMS
                logger.warn("WMS failed, compensating CMS for order: {}", orderMessage.getOrderNumber());
                cmsIntegrationService.cancelOrder(orderMessage.getOrderNumber());
                throw new RuntimeException("WMS processing failed: " + wmsResult.getMessage());
            }

            transaction.setWmsStatus("COMPLETED");
            transaction.setWmsResponse(wmsResult.getDetails());
            transactionRepository.save(transaction);

            // Step 3: Optimize route with ROS
            logger.info("Step 3: Optimizing route for order {} with ROS", orderMessage.getOrderNumber());
            transaction.setRosStatus("PROCESSING");
            transactionRepository.save(transaction);

            ProcessingResult rosResult = rosIntegrationService.optimizeRoute(orderMessage);
            if (!rosResult.isSuccess()) {
                // Compensate WMS and CMS
                logger.warn("ROS failed, compensating WMS and CMS for order: {}", orderMessage.getOrderNumber());
                wmsIntegrationService.removePackage(orderMessage.getOrderNumber());
                cmsIntegrationService.cancelOrder(orderMessage.getOrderNumber());
                throw new RuntimeException("ROS processing failed: " + rosResult.getMessage());
            }

            transaction.setRosStatus("COMPLETED");
            transaction.setRosResponse(rosResult.getDetails());
            transaction.setStatus("COMPLETED");
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Send success notification
            sendProcessingComplete(orderMessage, "SUCCESS", "Order processed successfully");

            logger.info("Order {} processing completed successfully", orderMessage.getOrderNumber());

        } catch (Exception e) {
            logger.error("Saga execution failed for order {}: {}", orderMessage.getOrderNumber(), e.getMessage());
            throw e;
        }
    }

    private void handleSagaFailure(IntegrationTransaction transaction, String errorMessage) {
        transaction.setStatus("FAILED");
        transaction.setErrorMessage(errorMessage);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Send failure notification back to order service
        sendProcessingComplete(
                createOrderMessageFromTransaction(transaction),
                "FAILED",
                errorMessage
        );

        logger.error("Order {} processing failed: {}", transaction.getOrderNumber(), errorMessage);
    }

    private void sendProcessingComplete(OrderMessage orderMessage, String status, String message) {
        try {
            Map<String, Object> statusUpdate = Map.of(
                    "orderNumber", orderMessage.getOrderNumber(),
                    "status", status,
                    "message", message,
                    "timestamp", LocalDateTime.now().toString()
            );

            rabbitTemplate.convertAndSend("order.exchange", "order.status.update", statusUpdate);
            logger.debug("Sent processing status update for order: {}", orderMessage.getOrderNumber());

        } catch (Exception e) {
            logger.error("Failed to send processing status update: {}", e.getMessage(), e);
        }
    }

    private OrderMessage mapToOrderMessage(Map<String, Object> orderData) {
        OrderMessage message = new OrderMessage();
        message.setOrderNumber((String) orderData.get("orderNumber"));
        message.setClientId((String) orderData.get("clientId"));
        message.setPickupAddress((String) orderData.get("pickupAddress"));
        message.setDeliveryAddress((String) orderData.get("deliveryAddress"));
        message.setPackageDescription((String) orderData.get("packageDescription"));
        message.setPriority((String) orderData.get("priority"));
        message.setContactNumber((String) orderData.get("contactNumber"));

        // Handle numeric fields safely
        Object weight = orderData.get("packageWeight");
        if (weight instanceof Number) {
            message.setPackageWeight(((Number) weight).doubleValue());
        }

        message.setSpecialInstructions((String) orderData.get("specialInstructions"));
        return message;
    }

    private OrderMessage createOrderMessageFromTransaction(IntegrationTransaction transaction) {
        OrderMessage message = new OrderMessage();
        message.setOrderNumber(transaction.getOrderNumber());
        return message;
    }

    public IntegrationTransaction getTransactionStatus(String orderNumber) {
        return transactionRepository.findByOrderNumber(orderNumber)
                .orElse(null);
    }

    /**
     * Get all transactions for monitoring/debugging
     */
    public java.util.List<IntegrationTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Get transactions by status
     */
    public java.util.List<IntegrationTransaction> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status);
    }

    /**
     * Retry failed transaction
     */
    public ProcessingResult retryTransaction(String orderNumber) {
        IntegrationTransaction transaction = getTransactionStatus(orderNumber);
        if (transaction == null) {
            return ProcessingResult.failure("Transaction not found for order: " + orderNumber);
        }

        if (!"FAILED".equals(transaction.getStatus())) {
            return ProcessingResult.failure("Can only retry failed transactions");
        }

        try {
            // Reset transaction status
            transaction.setStatus("RETRY");
            transaction.setErrorMessage(null);
            transactionRepository.save(transaction);

            // Create order message and retry
            OrderMessage orderMessage = createOrderMessageFromTransaction(transaction);
            // You would need to reconstruct the full order message from stored data
            // For now, we'll just return a placeholder

            executeSagaSteps(transaction, orderMessage);
            return ProcessingResult.success("Transaction retry completed successfully");

        } catch (Exception e) {
            handleSagaFailure(transaction, "Retry failed: " + e.getMessage());
            return ProcessingResult.failure("Transaction retry failed: " + e.getMessage());
        }
    }
}