// services/integration-service/src/main/java/com/swiftlogistics/integration/controller/ROSMockController.java
package com.swiftlogistics.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlogistics.integration.dto.RouteOptimizationRequest;
import com.swiftlogistics.integration.dto.RouteOptimizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/mock/ros")
public class ROSMockController {

    private static final Logger logger = LoggerFactory.getLogger(ROSMockController.class);
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String validApiKey = "ros-api-key-12345";

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoute(@RequestBody RouteOptimizationRequest request,
                                           @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        logger.info("ROS Mock: Received route optimization request for order: {}", request.getOrderNumber());

        // Validate API key - FIXED: More lenient validation
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("ROS Mock: Missing API key for order: {}", request.getOrderNumber());
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Missing API key",
                    "code", "UNAUTHORIZED"
            ));
        }

        // Accept any non-empty API key for demo purposes
        if (!apiKey.equals(validApiKey) && !apiKey.equals("test-api-key")) {
            logger.warn("ROS Mock: Invalid API key '{}' for order: {}", apiKey, request.getOrderNumber());
            // For demo, we'll still process the request but log the warning
            logger.info("ROS Mock: Proceeding with invalid API key for demo purposes");
        }

        // Simulate processing time
        try {
            Thread.sleep(500 + random.nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 5% failure rate
        if (random.nextInt(20) == 0) {
            logger.warn("ROS Mock: Simulating optimization failure for order: {}", request.getOrderNumber());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Route optimization failed",
                    "code", "OPTIMIZATION_ERROR",
                    "orderNumber", request.getOrderNumber()
            ));
        }

        // Generate optimized route response
        RouteOptimizationResponse response = new RouteOptimizationResponse();
        response.setOrderNumber(request.getOrderNumber());
        response.setOptimizedRoute(generateOptimizedRoute(request.getPickupAddress(), request.getDeliveryAddress()));
        response.setEstimatedDuration(15 + random.nextInt(45)); // 15-60 minutes
        response.setEstimatedDistance((2.5 + random.nextDouble() * 20)); // 2.5-22.5 km
        response.setOptimizationId("OPT-" + System.currentTimeMillis());
        response.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        logger.info("ROS Mock: Route optimized for order {} - Duration: {} mins, Distance: {} km",
                request.getOrderNumber(), response.getEstimatedDuration(),
                String.format("%.1f", response.getEstimatedDistance()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "ROS Mock",
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "note", "Use API key 'ros-api-key-12345' or 'test-api-key'"
        ));
    }

    private String generateOptimizedRoute(String pickup, String delivery) {
        String[] waypoints = {
                "Warehouse District",
                "Main Street Junction",
                "City Center",
                "Residential Area",
                "Commercial Zone"
        };

        String startPoint = pickup != null ? pickup : "Pickup Location";
        String endPoint = delivery != null ? delivery : "Delivery Location";
        String waypoint = waypoints[random.nextInt(waypoints.length)];

        return String.format("%s → %s → %s", startPoint, waypoint, endPoint);
    }
}