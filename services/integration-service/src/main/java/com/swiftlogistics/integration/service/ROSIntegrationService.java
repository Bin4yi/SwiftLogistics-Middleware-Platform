// services/integration-service/src/main/java/com/swiftlogistics/integration/service/ROSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import com.swiftlogistics.integration.dto.RouteOptimizationRequest;
import com.swiftlogistics.integration.dto.RouteOptimizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ROSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ROSIntegrationService.class);

    @Value("${external-systems.ros.endpoint:http://localhost:8082/mock/ros}")
    private String rosEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey = "test-api-key"; // In production, this would be from config

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ProcessingResult optimizeRoute(OrderMessage orderMessage) {
        logger.info("Optimizing route for order {} with ROS (REST)", orderMessage.getOrderNumber());

        try {
            RouteOptimizationRequest request = new RouteOptimizationRequest(
                    orderMessage.getOrderNumber(),
                    orderMessage.getPickupAddress(),
                    orderMessage.getDeliveryAddress()
            );
            request.setPriority(orderMessage.getPriority());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);
            HttpEntity<RouteOptimizationRequest> httpRequest =
                    new HttpEntity<>(request, headers);

            ResponseEntity<RouteOptimizationResponse> response = restTemplate.postForEntity(
                    rosEndpoint + "/optimize",
                    httpRequest,
                    RouteOptimizationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                RouteOptimizationResponse rosResponse = response.getBody();
                logger.info("ROS route optimization successful for order: {} - Route: {}",
                        orderMessage.getOrderNumber(), rosResponse.getOptimizedRoute());
                return ProcessingResult.success(
                        "Route optimized successfully",
                        "Route: " + rosResponse.getOptimizedRoute() +
                                ", Duration: " + rosResponse.getEstimatedDuration() + " mins"
                );
            } else {
                logger.error("ROS route optimization failed for: {}", orderMessage.getOrderNumber());
                return ProcessingResult.failure("ROS optimization failed", "No valid response received");
            }

        } catch (Exception e) {
            logger.error("Error optimizing route for order {} with ROS: {}",
                    orderMessage.getOrderNumber(), e.getMessage(), e);
            return ProcessingResult.failure("ROS optimization error: " + e.getMessage());
        }
    }
}