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

    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoute(@RequestBody RouteOptimizationRequest request,
                                           @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        logger.info("ROS Mock: Received route optimization request for order: {}", request.getOrderNumber());

        // Validate API key
        if (!"swift-logistics-key".equals(apiKey)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid API key", "code", "UNAUTHORIZED"));
        }

        // Simulate processing time
        try {
            Thread.sleep(1500 + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 5% failure rate
        if (random.nextInt(20) == 0) {
            logger.warn("ROS Mock: Simulating optimization failure for order: {}", request.getOrderNumber());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Route optimization failed",
                            "code", "OPTIMIZATION_FAILED",
                            "orderNumber", request.getOrderNumber()
                    ));
        }

        // Create optimized route response
        RouteOptimizationResponse response = new RouteOptimizationResponse();
        response.setOrderNumber(request.getOrderNumber());
        response.setRouteId("ROUTE-" + System.currentTimeMillis());
        response.setEstimatedDeliveryTime(
                LocalDateTime.now().plusHours(2 + random.nextInt(6))
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        response.setEstimatedDistance(5.0 + random.nextDouble() * 50.0);
        response.setEstimatedDuration(30 + random.nextInt(120));
        response.setAssignedVehicle("VAN-" + (100 + random.nextInt(50)));
        response.setDriverId("DRV-" + (1000 + random.nextInt(500)));
        response.setRoutePoints(Arrays.asList(
                request.getPickupAddress(),
                "Warehouse Hub - Colombo",
                "Distribution Center - " + getRandomArea(),
                request.getDeliveryAddress()
        ));

        logger.info("ROS Mock: Route optimized for order: {}. Driver: {}, ETA: {}",
                request.getOrderNumber(), response.getDriverId(), response.getEstimatedDeliveryTime());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/remove/{orderNumber}")
    public ResponseEntity<?> removeFromRoute(@PathVariable String orderNumber,
                                             @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        logger.info("ROS Mock: Received route removal request for order: {}", orderNumber);

        if (!"swift-logistics-key".equals(apiKey)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid API key"));
        }

        // Simulate processing
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("ROS Mock: Order {} removed from route successfully", orderNumber);

        return ResponseEntity.ok(Map.of(
                "result", "SUCCESS",
                "orderNumber", orderNumber,
                "message", "Order removed from route",
                "timestamp", LocalDateTime.now()
        ));
    }

    @GetMapping("/status/{orderNumber}")
    public ResponseEntity<?> getRouteStatus(@PathVariable String orderNumber,
                                            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        logger.debug("ROS Mock: Route status request for order: {}", orderNumber);

        if (!"swift-logistics-key".equals(apiKey)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid API key"));
        }

        return ResponseEntity.ok(Map.of(
                "orderNumber", orderNumber,
                "status", "IN_ROUTE",
                "currentLocation", "En route to " + getRandomArea(),
                "estimatedArrival", LocalDateTime.now().plusMinutes(45 + random.nextInt(120)),
                "driverId", "DRV-" + (1000 + random.nextInt(500))
        ));
    }

    private String getRandomArea() {
        String[] areas = {"Colombo", "Nugegoda", "Maharagama", "Kandy", "Gampaha", "Kalutara", "Mount Lavinia"};
        return areas[random.nextInt(areas.length)];
    }
}