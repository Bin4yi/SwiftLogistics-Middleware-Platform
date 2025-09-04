// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/controller/RealTimeTrackingController.java

package com.swiftlogistics.tracking.controller;

import com.swiftlogistics.tracking.dto.RealTimeTrackingResponse;
import com.swiftlogistics.tracking.service.RealTimeTrackingService;
// REMOVED: import com.swiftlogistics.tracking.service.TrackingService; // This class doesn't exist
import com.swiftlogistics.tracking.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class RealTimeTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeTrackingController.class);

    @Autowired
    private RealTimeTrackingService realTimeTrackingService;

    // REMOVED: @Autowired private TrackingService trackingService; // This class doesn't exist

    @Autowired
    private WebSocketSessionManager sessionManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.info("Real-time tracking health check requested");

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "UP");
            response.put("service", "realtime-tracking");
            response.put("timestamp", LocalDateTime.now());
            response.put("version", "1.0.0");
            response.put("features", java.util.List.of(
                    "WebSocket connections",
                    "Real-time order tracking",
                    "Driver location updates",
                    "Live notifications",
                    "Multi-client support"
            ));

            // Add connection statistics
            Map<String, Object> connectionStats = sessionManager.getConnectionStats();
            response.put("connections", connectionStats);
            response.put("realTimeEnabled", sessionManager.hasActiveConnections());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Real-time tracking health check failed: {}", e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "DOWN");
            response.put("service", "realtime-tracking");
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(503).body(response);
        }
    }

    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getRealtimeTracking(@PathVariable String orderNumber) {
        logger.info("Real-time tracking requested for order: {}", orderNumber);

        try {
            Map<String, Object> trackingData = realTimeTrackingService.getRealtimeTrackingData(orderNumber);

            if (trackingData.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }

            // Add real-time connection info
            trackingData.put("realTimeEnabled", sessionManager.hasActiveConnections());
            trackingData.put("webSocketEndpoint", "/ws/tracking");
            trackingData.put("instructions", Map.of(
                    "connect", "ws://localhost:8084/ws/tracking",
                    "subscribe", Map.of(
                            "type", "subscribe_order",
                            "orderNumber", orderNumber
                    )
            ));

            return ResponseEntity.ok(trackingData);

        } catch (Exception e) {
            logger.error("Error getting real-time tracking for order {}: {}", orderNumber, e.getMessage(), e);

            Map<String, Object> error = Map.of(
                    "error", "Failed to get tracking data",
                    "message", e.getMessage(),
                    "orderNumber", orderNumber
            );

            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/driver/{driverId}/location")
    public ResponseEntity<Map<String, Object>> getDriverLocation(@PathVariable String driverId) {
        logger.info("Driver location requested for: {}", driverId);

        try {
            Map<String, Object> locationData = realTimeTrackingService.getDriverLocationData(driverId);

            if (locationData.containsKey("error")) {
                return ResponseEntity.notFound().build();
            }

            // Add real-time subscription info
            locationData.put("realTimeEnabled", sessionManager.hasActiveConnections());
            locationData.put("webSocketEndpoint", "/ws/tracking");
            locationData.put("instructions", Map.of(
                    "connect", "ws://localhost:8084/ws/tracking",
                    "subscribe", Map.of(
                            "type", "subscribe_driver",
                            "driverId", driverId
                    )
            ));

            return ResponseEntity.ok(locationData);

        } catch (Exception e) {
            logger.error("Error getting driver location for {}: {}", driverId, e.getMessage(), e);

            Map<String, Object> error = Map.of(
                    "error", "Failed to get driver location",
                    "message", e.getMessage(),
                    "driverId", driverId
            );

            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/connections/stats")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        logger.info("Connection statistics requested");

        try {
            Map<String, Object> stats = sessionManager.getConnectionStats();
            stats.put("timestamp", LocalDateTime.now());
            stats.put("service", "realtime-tracking");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error getting connection stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to get connection statistics")
            );
        }
    }

    @PostMapping("/test/broadcast")
    public ResponseEntity<Map<String, Object>> testBroadcast(@RequestBody Map<String, Object> testMessage) {
        logger.info("Test broadcast requested: {}", testMessage);

        try {
            String type = (String) testMessage.get("type");
            String targetId = (String) testMessage.get("targetId");
            Object data = testMessage.get("data");

            if (type == null || targetId == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Type and targetId are required")
                );
            }

            int beforeCount = sessionManager.getActiveSessionCount();

            switch (type.toLowerCase()) {
                case "order":
                    sessionManager.broadcastOrderUpdate(targetId, data);
                    break;
                case "driver":
                    sessionManager.broadcastDriverUpdate(targetId, data);
                    break;
                case "client":
                    sessionManager.broadcastClientUpdate(targetId, data);
                    break;
                default:
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Unknown broadcast type: " + type)
                    );
            }

            Map<String, Object> response = Map.of(
                    "success", true,
                    "type", type,
                    "targetId", targetId,
                    "activeSessions", beforeCount,
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in test broadcast: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Broadcast failed: " + e.getMessage())
            );
        }
    }

    @GetMapping("/websocket/info")
    public ResponseEntity<Map<String, Object>> getWebSocketInfo() {
        Map<String, Object> info = Map.of(
                "endpoints", Map.of(
                        "sockjs", "/ws/tracking",
                        "native", "/websocket/tracking"
                ),
                "subscriptionTypes", java.util.List.of(
                        Map.of(
                                "type", "subscribe_order",
                                "example", Map.of("type", "subscribe_order", "orderNumber", "ORD-001")
                        ),
                        Map.of(
                                "type", "subscribe_driver",
                                "example", Map.of("type", "subscribe_driver", "driverId", "DRV-001")
                        ),
                        Map.of(
                                "type", "subscribe_client",
                                "example", Map.of("type", "subscribe_client", "clientId", "CLIENT-001")
                        )
                ),
                "messageTypes", java.util.List.of(
                        "connection_established",
                        "subscription_confirmed",
                        "order_update",
                        "driver_update",
                        "client_update",
                        "error",
                        "pong"
                ),
                "connectionStats", sessionManager.getConnectionStats()
        );

        return ResponseEntity.ok(info);
    }

    // Handle OPTIONS requests for CORS
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }
}