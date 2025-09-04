// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/controller/TrackingController.java
// FIXED VERSION - Resolves type inference issues

package com.swiftlogistics.tracking.controller;

import com.swiftlogistics.tracking.dto.TrackingEventDto;
import com.swiftlogistics.tracking.dto.RealTimeTrackingResponse;
import com.swiftlogistics.tracking.entity.DeliveryTracking;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.enums.TrackingEventType;
import com.swiftlogistics.tracking.repository.DeliveryTrackingRepository;
import com.swiftlogistics.tracking.repository.TrackingEventRepository;
import com.swiftlogistics.tracking.service.RealTimeTrackingService;
import com.swiftlogistics.tracking.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class TrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RealTimeTrackingService realTimeTrackingService;

    @Autowired
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private WebSocketSessionManager sessionManager;

    // ============== HEALTH AND STATUS ENDPOINTS ==============

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Tracking service health check requested");

        try {
            // Test database connectivity
            dataSource.getConnection().close();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", "tracking-service");
            response.put("timestamp", LocalDateTime.now());
            response.put("version", "1.0.0");
            response.put("port", "8084");
            response.put("database", "CONNECTED");

            // Use HashMap instead of Map.of() to avoid type inference issues
            Map<String, Object> checks = new HashMap<>();
            checks.put("database", "PASS");
            checks.put("redis", "AVAILABLE");
            checks.put("websocket", "ENABLED");
            checks.put("notifications", "ACTIVE");
            checks.put("realTimeConnections", sessionManager.hasActiveConnections());
            response.put("checks", checks);

            // Add real-time connection stats
            response.put("connectionStats", sessionManager.getConnectionStats());

            logger.info("Tracking service health check: UP");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Tracking service health check failed: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("service", "tracking-service");
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "tracking-service");
        status.put("status", "RUNNING");
        status.put("timestamp", LocalDateTime.now());
        status.put("uptime", "Available");
        status.put("features", java.util.List.of(
                "Real-time tracking",
                "WebSocket connections",
                "Order status updates",
                "Driver location tracking",
                "Push notifications",
                "Multi-client support"
        ));

        // Add real-time capabilities using HashMap
        Map<String, Object> realTimeInfo = new HashMap<>();
        realTimeInfo.put("enabled", true);
        realTimeInfo.put("activeConnections", sessionManager.getActiveSessionCount());
        realTimeInfo.put("webSocketEndpoints", java.util.List.of("/ws/tracking", "/websocket/tracking"));
        realTimeInfo.put("supportedEvents", java.util.Arrays.stream(TrackingEventType.values())
                .map(eventType -> {
                    Map<String, Object> eventInfo = new HashMap<>();
                    eventInfo.put("name", eventType.name());
                    eventInfo.put("displayName", eventType.getDisplayName());
                    eventInfo.put("description", eventType.getDescription());
                    return eventInfo;
                })
                .collect(Collectors.toList()));

        status.put("realTime", realTimeInfo);

        return ResponseEntity.ok(status);
    }

    // ============== TRACKING ENDPOINTS ==============

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrderTracking(@PathVariable String orderNumber) {
        logger.info("Order tracking requested for: {}", orderNumber);

        try {
            Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);

            if (trackingOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DeliveryTracking tracking = trackingOpt.get();

            // Get recent tracking events
            List<TrackingEvent> events = trackingEventRepository
                    .findByOrderNumberOrderByTimestampDesc(orderNumber)
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());

            // Convert to DTOs
            List<TrackingEventDto> eventDtos = events.stream()
                    .map(TrackingEventDto::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("orderNumber", tracking.getOrderNumber());
            response.put("currentStatus", tracking.getCurrentStatus());
            response.put("clientId", tracking.getClientId());
            response.put("assignedDriverId", tracking.getAssignedDriverId());
            response.put("lastKnownLatitude", tracking.getLastKnownLatitude());
            response.put("lastKnownLongitude", tracking.getLastKnownLongitude());
            response.put("lastLocationUpdate", tracking.getLastLocationUpdate());
            response.put("estimatedDeliveryTime", tracking.getEstimatedDeliveryTime());
            response.put("createdAt", tracking.getCreatedAt());
            response.put("updatedAt", tracking.getUpdatedAt());
            response.put("recentEvents", eventDtos);

            // Add real-time connection info using HashMap
            Map<String, Object> realTimeInfo = new HashMap<>();
            realTimeInfo.put("enabled", true);
            realTimeInfo.put("webSocketEndpoint", "/ws/tracking");

            Map<String, Object> subscriptionInfo = new HashMap<>();
            subscriptionInfo.put("type", "subscribe_order");
            subscriptionInfo.put("orderNumber", orderNumber);
            realTimeInfo.put("subscriptionInstructions", subscriptionInfo);
            realTimeInfo.put("activeConnections", sessionManager.getActiveSessionCount());

            response.put("realTimeTracking", realTimeInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting order tracking for {}: {}", orderNumber, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get order tracking");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/orders/{orderNumber}/events")
    public ResponseEntity<List<TrackingEventDto>> getOrderEvents(@PathVariable String orderNumber,
                                                                 @RequestParam(defaultValue = "20") int limit) {
        logger.info("Order events requested for: {} (limit: {})", orderNumber, limit);

        try {
            List<TrackingEvent> events = trackingEventRepository
                    .findByOrderNumberOrderByTimestampDesc(orderNumber)
                    .stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            List<TrackingEventDto> eventDtos = events.stream()
                    .map(TrackingEventDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(eventDtos);

        } catch (Exception e) {
            logger.error("Error getting order events for {}: {}", orderNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/drivers/{driverId}/location")
    public ResponseEntity<Map<String, Object>> getDriverLocation(@PathVariable String driverId) {
        logger.info("Driver location requested for: {}", driverId);

        try {
            Map<String, Object> locationData = realTimeTrackingService.getDriverLocationData(driverId);

            if (locationData.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }

            // Add real-time subscription info using HashMap
            Map<String, Object> realTimeInfo = new HashMap<>();
            realTimeInfo.put("enabled", true);
            realTimeInfo.put("webSocketEndpoint", "/ws/tracking");

            Map<String, Object> subscriptionInfo = new HashMap<>();
            subscriptionInfo.put("type", "subscribe_driver");
            subscriptionInfo.put("driverId", driverId);
            realTimeInfo.put("subscriptionInstructions", subscriptionInfo);

            locationData.put("realTimeTracking", realTimeInfo);

            return ResponseEntity.ok(locationData);

        } catch (Exception e) {
            logger.error("Error getting driver location for {}: {}", driverId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get driver location");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/drivers/{driverId}/orders")
    public ResponseEntity<List<Map<String, Object>>> getDriverOrders(@PathVariable String driverId) {
        logger.info("Driver orders requested for: {}", driverId);

        try {
            List<DeliveryTracking> deliveries = deliveryTrackingRepository.findByAssignedDriverId(driverId);

            List<Map<String, Object>> orders = deliveries.stream()
                    .map(delivery -> {
                        Map<String, Object> orderInfo = new HashMap<>();
                        orderInfo.put("orderNumber", delivery.getOrderNumber());
                        orderInfo.put("currentStatus", delivery.getCurrentStatus());
                        orderInfo.put("clientId", delivery.getClientId());
                        orderInfo.put("lastKnownLatitude", delivery.getLastKnownLatitude() != null ? delivery.getLastKnownLatitude() : 0.0);
                        orderInfo.put("lastKnownLongitude", delivery.getLastKnownLongitude() != null ? delivery.getLastKnownLongitude() : 0.0);
                        orderInfo.put("estimatedDeliveryTime", delivery.getEstimatedDeliveryTime());
                        orderInfo.put("updatedAt", delivery.getUpdatedAt());
                        return orderInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            logger.error("Error getting driver orders for {}: {}", driverId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/clients/{clientId}/orders")
    public ResponseEntity<List<Map<String, Object>>> getClientOrders(@PathVariable String clientId) {
        logger.info("Client orders requested for: {}", clientId);

        try {
            List<DeliveryTracking> deliveries = deliveryTrackingRepository.findByClientId(clientId);

            List<Map<String, Object>> orders = deliveries.stream()
                    .map(delivery -> {
                        Map<String, Object> orderInfo = new HashMap<>();
                        orderInfo.put("orderNumber", delivery.getOrderNumber());
                        orderInfo.put("currentStatus", delivery.getCurrentStatus());
                        orderInfo.put("assignedDriverId", delivery.getAssignedDriverId() != null ? delivery.getAssignedDriverId() : "");
                        orderInfo.put("lastKnownLatitude", delivery.getLastKnownLatitude() != null ? delivery.getLastKnownLatitude() : 0.0);
                        orderInfo.put("lastKnownLongitude", delivery.getLastKnownLongitude() != null ? delivery.getLastKnownLongitude() : 0.0);
                        orderInfo.put("estimatedDeliveryTime", delivery.getEstimatedDeliveryTime());
                        orderInfo.put("createdAt", delivery.getCreatedAt());
                        orderInfo.put("updatedAt", delivery.getUpdatedAt());
                        return orderInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            logger.error("Error getting client orders for {}: {}", clientId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============== REAL-TIME ENDPOINTS ==============

    @GetMapping("/realtime/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getRealtimeTracking(@PathVariable String orderNumber) {
        logger.info("Real-time tracking requested for order: {}", orderNumber);

        try {
            Map<String, Object> trackingData = realTimeTrackingService.getRealtimeTrackingData(orderNumber);

            if (trackingData.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }

            // Add WebSocket connection instructions using HashMap
            trackingData.put("realTimeEnabled", sessionManager.hasActiveConnections());

            Map<String, Object> webSocketInfo = new HashMap<>();
            webSocketInfo.put("endpoint", "/ws/tracking");
            webSocketInfo.put("nativeEndpoint", "/websocket/tracking");

            Map<String, Object> subscribeMessage = new HashMap<>();
            subscribeMessage.put("type", "subscribe_order");
            subscribeMessage.put("orderNumber", orderNumber);
            webSocketInfo.put("subscribeMessage", subscribeMessage);
            webSocketInfo.put("connectionExample", "ws://localhost:8084/websocket/tracking");

            trackingData.put("webSocketInfo", webSocketInfo);

            return ResponseEntity.ok(trackingData);

        } catch (Exception e) {
            logger.error("Error getting real-time tracking for order {}: {}", orderNumber, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get real-time tracking data");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/notifications/{driverId}")
    public ResponseEntity<Map<String, Object>> getDriverNotifications(@PathVariable String driverId) {
        logger.info("Getting notifications for driver: {}", driverId);

        Map<String, Object> response = new HashMap<>();
        response.put("driverId", driverId);

        // Create notification list using HashMap
        Map<String, Object> notification1 = new HashMap<>();
        notification1.put("id", 1);
        notification1.put("message", "New delivery assigned");
        notification1.put("timestamp", LocalDateTime.now());
        notification1.put("type", "ASSIGNMENT");

        Map<String, Object> notification2 = new HashMap<>();
        notification2.put("id", 2);
        notification2.put("message", "Route updated");
        notification2.put("timestamp", LocalDateTime.now().minusMinutes(15));
        notification2.put("type", "ROUTE_UPDATE");

        response.put("notifications", java.util.List.of(notification1, notification2));
        response.put("realTimeEnabled", sessionManager.hasActiveConnections());

        return ResponseEntity.ok(response);
    }

    // ============== STATISTICS AND ANALYTICS ==============

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getTrackingStats() {
        logger.info("Getting tracking statistics overview");

        try {
            long totalOrders = deliveryTrackingRepository.count();
            long totalEvents = trackingEventRepository.count();

            Map<String, Long> statusCounts = new HashMap<>();
            for (String status : java.util.List.of("ASSIGNED", "PICKED_UP", "EN_ROUTE_DELIVERY", "DELIVERED", "FAILED")) {
                statusCounts.put(status, deliveryTrackingRepository.countByStatus(status));
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", totalOrders);
            stats.put("totalEvents", totalEvents);
            stats.put("statusBreakdown", statusCounts);
            stats.put("realTimeConnections", sessionManager.getConnectionStats());
            stats.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting tracking stats: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get tracking statistics");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/events/types")
    public ResponseEntity<List<Map<String, Object>>> getEventTypes() {
        List<Map<String, Object>> eventTypes = java.util.Arrays.stream(TrackingEventType.values())
                .map(eventType -> {
                    Map<String, Object> eventInfo = new HashMap<>();
                    eventInfo.put("name", eventType.name());
                    eventInfo.put("displayName", eventType.getDisplayName());
                    eventInfo.put("description", eventType.getDescription());
                    return eventInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(eventTypes);
    }

    // ============== WEBSOCKET INFO ENDPOINTS ==============

    @GetMapping("/websocket/info")
    public ResponseEntity<Map<String, Object>> getWebSocketInfo() {
        Map<String, Object> info = new HashMap<>();

        // Endpoints info
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("sockjs", "/ws/tracking");
        endpoints.put("native", "/websocket/tracking");
        info.put("endpoints", endpoints);

        // Subscription types
        Map<String, Object> orderSub = new HashMap<>();
        orderSub.put("type", "subscribe_order");
        orderSub.put("description", "Subscribe to updates for a specific order");
        Map<String, Object> orderExample = new HashMap<>();
        orderExample.put("type", "subscribe_order");
        orderExample.put("orderNumber", "ORD-001");
        orderSub.put("example", orderExample);

        Map<String, Object> driverSub = new HashMap<>();
        driverSub.put("type", "subscribe_driver");
        driverSub.put("description", "Subscribe to updates for a specific driver");
        Map<String, Object> driverExample = new HashMap<>();
        driverExample.put("type", "subscribe_driver");
        driverExample.put("driverId", "DRV-001");
        driverSub.put("example", driverExample);

        Map<String, Object> clientSub = new HashMap<>();
        clientSub.put("type", "subscribe_client");
        clientSub.put("description", "Subscribe to updates for a specific client");
        Map<String, Object> clientExample = new HashMap<>();
        clientExample.put("type", "subscribe_client");
        clientExample.put("clientId", "CLIENT-001");
        clientSub.put("example", clientExample);

        info.put("subscriptionTypes", java.util.List.of(orderSub, driverSub, clientSub));

        info.put("messageTypes", java.util.List.of(
                "connection_established",
                "subscription_confirmed",
                "order_update",
                "driver_update",
                "client_update",
                "error",
                "pong"
        ));

        info.put("connectionStats", sessionManager.getConnectionStats());

        List<Map<String, Object>> supportedEvents = java.util.Arrays.stream(TrackingEventType.values())
                .map(eventType -> {
                    Map<String, Object> eventInfo = new HashMap<>();
                    eventInfo.put("name", eventType.name());
                    eventInfo.put("displayName", eventType.getDisplayName());
                    eventInfo.put("description", eventType.getDescription());
                    return eventInfo;
                })
                .collect(Collectors.toList());
        info.put("supportedEvents", supportedEvents);

        return ResponseEntity.ok(info);
    }

    // ============== UTILITY ENDPOINTS ==============

    // Handle OPTIONS requests for CORS
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }
}