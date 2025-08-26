// services/integration-service/src/main/java/com/swiftlogistics/integration/service/WMSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Service
public class WMSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(WMSIntegrationService.class);

    @Value("${external-systems.wms.host}")
    private String wmsHost;

    @Value("${external-systems.wms.port}")
    private int wmsPort;

    @Value("${external-systems.wms.timeout}")
    private int wmsTimeout;

    // For demo purposes, we'll simulate TCP/IP with REST calls to our mock endpoint
    private final String mockWmsEndpoint = "http://localhost:8082/mock/wms";

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
            return ProcessingResult.failure("WMS integration error: " + e.getMessage());
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
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
            logger.error("Error removing package from WMS for order {}: {}",
                    orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("WMS removal error: " + e.getMessage());
        }
    }

    public ProcessingResult getPackageStatus(String orderNumber) {
        logger.debug("Getting package status for order: {}", orderNumber);

        try {
            String tcpMessage = createTcpMessage("GET_STATUS", orderNumber);
            String response = simulateTcpCall("getStatus", tcpMessage);

            if (response.contains("SUCCESS")) {
                return ProcessingResult.success("Package status retrieved", response);
            } else {
                return ProcessingResult.failure("Failed to get package status: " + response);
            }

        } catch (Exception e) {
            logger.error("Error getting package status for order {}: {}",
                    orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("WMS status check error: " + e.getMessage());
        }
    }

    // Real TCP/IP implementation (commented out for demo)
    /*
    private String sendTcpMessage(String message) throws IOException {
        try (Socket socket = new Socket(wmsHost, wmsPort)) {
            socket.setSoTimeout(wmsTimeout);

            // Send message
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);

            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                if (line.contains("END")) {
                    break;
                }
            }

            return response.toString();

        } catch (SocketTimeoutException e) {
            throw new IOException("WMS TCP connection timeout", e);
        }
    }
    */

    private String createTcpMessage(String command, Object data) {
        StringBuilder message = new StringBuilder();
        message.append("SWIFT_WMS_PROTOCOL_V1\n");
        message.append("COMMAND:").append(command).append("\n");
        message.append("TIMESTAMP:").append(System.currentTimeMillis()).append("\n");

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
            // In a real implementation, this would be a TCP socket call
            // For demo, we're using REST to simulate
            org.springframework.web.client.RestTemplate restTemplate =
                    new org.springframework.web.client.RestTemplate();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);

            org.springframework.http.HttpEntity<String> request =
                    new org.springframework.http.HttpEntity<>(tcpMessage, headers);

            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.postForEntity(mockWmsEndpoint + "/" + endpoint, request, String.class);

            return response.getBody();

        } catch (Exception e) {
            logger.error("Error in TCP simulation call: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }
}