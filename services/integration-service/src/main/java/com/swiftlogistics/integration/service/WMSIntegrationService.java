// services/integration-service/src/main/java/com/swiftlogistics/integration/service/WMSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Service
public class WMSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(WMSIntegrationService.class);

    @Value("${external-systems.wms.host:localhost}")
    private String wmsHost;

    @Value("${external-systems.wms.port:9999}")
    private int wmsPort;

    @Value("${external-systems.wms.timeout:15000}")
    private int wmsTimeout;

    // For demo purposes, we'll simulate TCP/IP with REST calls to our mock endpoint
    private final String mockWmsEndpoint = "http://localhost:8082/mock/wms";
    private final RestTemplate restTemplate = new RestTemplate();

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ProcessingResult addPackage(OrderMessage orderMessage) {
        logger.info("Adding package for order {} to WMS (TCP/IP)", orderMessage.getOrderNumber());

        try {
            // Simulate TCP/IP protocol message
            String tcpMessage = createTcpMessage("ADD_PACKAGE", orderMessage);

            // For demo, we'll use HTTP to simulate the TCP call
            String response = simulateTcpCall("addPackage", tcpMessage);

            if (response.contains("SUCCESS")) {
                logger.info("WMS package addition successful for order: {}", orderMessage.getOrderNumber());
                return ProcessingResult.success("Package added to WMS", response);
            } else {
                logger.error("WMS package addition failed for order: {}", orderMessage.getOrderNumber());
                return ProcessingResult.failure("WMS package addition failed: " + response);
            }

        } catch (Exception e) {
            logger.error("Error adding package to WMS for order {}: {}",
                    orderMessage.getOrderNumber(), e.getMessage(), e);
            return ProcessingResult.failure("WMS error: " + e.getMessage());
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ProcessingResult removePackage(String orderNumber) {
        logger.info("Removing package for order {} from WMS (compensation)", orderNumber);

        try {
            String tcpMessage = createTcpMessage("REMOVE_PACKAGE", orderNumber);
            String response = simulateTcpCall("removePackage", tcpMessage);

            if (response.contains("SUCCESS")) {
                logger.info("WMS package removal successful for order: {}", orderNumber);
                return ProcessingResult.success("Package removed from WMS", response);
            } else {
                logger.error("WMS package removal failed for order: {}", orderNumber);
                return ProcessingResult.failure("WMS package removal failed: " + response);
            }

        } catch (Exception e) {
            logger.error("Error removing package from WMS for order {}: {}", orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("WMS removal error: " + e.getMessage());
        }
    }

    private String createTcpMessage(String operation, Object data) {
        StringBuilder message = new StringBuilder();
        message.append("OPERATION:").append(operation).append("\n");

        if (data instanceof OrderMessage) {
            OrderMessage order = (OrderMessage) data;
            message.append("ORDER_NUMBER:").append(order.getOrderNumber()).append("\n");
            message.append("CLIENT_ID:").append(order.getClientId()).append("\n");
            message.append("PICKUP_ADDRESS:").append(order.getPickupAddress()).append("\n");
            message.append("DELIVERY_ADDRESS:").append(order.getDeliveryAddress()).append("\n");
            message.append("PACKAGE_DESC:").append(order.getPackageDescription()).append("\n");
            message.append("PRIORITY:").append(order.getPriority()).append("\n");
        } else if (data instanceof String) {
            message.append("ORDER_NUMBER:").append(data).append("\n");
        }

        message.append("END\n");
        return message.toString();
    }

    // Simulate TCP call with HTTP for demo purposes
    private String simulateTcpCall(String endpoint, String tcpMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> request = new HttpEntity<>(tcpMessage, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    mockWmsEndpoint + "/" + endpoint,
                    request,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Error in TCP simulation call: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Real TCP/IP implementation - commented out for demo
     * In production, this would replace the simulateTcpCall method
     */
    /*
    private String realTcpCall(String message) throws IOException {
        try (Socket socket = new Socket(wmsHost, wmsPort)) {
            socket.setSoTimeout(wmsTimeout);

            // Send message
            try (PrintWriter out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                out.println(message);
            }

            // Read response
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                return in.readLine();
            }
        } catch (SocketTimeoutException e) {
            throw new IOException("WMS TCP call timeout", e);
        }
    }
    */
}