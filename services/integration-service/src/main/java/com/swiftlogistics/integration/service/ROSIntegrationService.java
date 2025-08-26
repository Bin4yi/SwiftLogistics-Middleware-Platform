// services/integration-service/src/main/java/com/swiftlogistics/integration/service/ROSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import com.swiftlogistics.integration.dto.RouteOptimizationRequest;
import com.swiftlogistics.integration.dto.RouteOptimizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ROSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ROSIntegrationService.class);

    @Value("${external-systems.ros.endpoint}")
    private String rosEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public ProcessingResult optimizeRoute(OrderMessage orderMessage) {
        logger.info("Optimizing route for order {} with ROS (REST)", orderMessage.getOrderNumber());

        try {
            // Create REST JSON request
            RouteOptimizationRequest request = new RouteOptimizationRequest();
            request.setOrderNumber(orderMessage.getOrderNumber());
            request.setPickupAddress(orderMessage.getPickupAddress());
            request.setDeliveryAddress(orderMessage.getDeliveryAddress());
            request.setPriority(orderMessage.getPriority());
            request.setPackageWeight(2.5); // Mock weight
            request.setVehicleType("VAN");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", "swift-logistics-key");

            String jsonRequest = objectMapper.writeValueAsString(request);
            HttpEntity<String> httpEntity = new HttpEntity<>(jsonRequest, headers);

            // Call mock ROS endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(
                    rosEndpoint + "/optimize", httpEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                RouteOptimizationResponse rosResponse = objectMapper.readValue(responseBody, RouteOptimizationResponse.class);

                logger.info("ROS optimization successful for order: {}. Estimated delivery: {}",
                        orderMessage.getOrderNumber(), rosResponse.getEstimatedDeliveryTime());

                return ProcessingResult.success("Route optimized successfully", responseBody);
            } else {
                logger.error("ROS optimization failed with status: {}", response.getStatusCode());
                return ProcessingResult.failure("ROS optimization failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error calling ROS for order {}: {}", orderMessage.getOrderNumber(), e.getMessage(), e);
            return ProcessingResult.failure("ROS integration error: " + e.getMessage());
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public ProcessingResult removeFromRoute(String orderNumber) {
        logger.info("Removing order {} from route (compensation)", orderNumber);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", "swift-logistics-key");

            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    rosEndpoint + "/remove/" + orderNumber, httpEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("ROS route removal successful for order: {}", orderNumber);
                return ProcessingResult.success("Order removed from route", response.getBody());
            } else {
                logger.error("ROS route removal failed with status: {}", response.getStatusCode());
                return ProcessingResult.failure("ROS route removal failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error removing order {} from route: {}", orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("ROS route removal error: " + e.getMessage());
        }
    }

    public ProcessingResult getRouteStatus(String orderNumber) {
        logger.debug("Getting route status for order: {}", orderNumber);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", "swift-logistics-key");

            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    rosEndpoint + "/status/" + orderNumber, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ProcessingResult.success("Route status retrieved", response.getBody());
            } else {
                return ProcessingResult.failure("Failed to get route status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error getting route status for order {}: {}", orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("ROS status check error: " + e.getMessage());
        }
    }
}