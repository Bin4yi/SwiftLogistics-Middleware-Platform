// services/tracking-service/src/main/java/com/swiftlogistics/tracking/controller/WebSocketController.java
package com.swiftlogistics.tracking.controller;

import com.swiftlogistics.tracking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    private NotificationService notificationService;

    @MessageMapping("/tracking/subscribe")
    public void subscribeToTracking(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        String orderNumber = (String) message.get("orderNumber");
        String clientId = (String) message.get("clientId");

        logger.info("Client {} subscribed to tracking for order: {}", clientId, orderNumber);

        // Store subscription information in session attributes
        headerAccessor.getSessionAttributes().put("orderNumber", orderNumber);
        headerAccessor.getSessionAttributes().put("clientId", clientId);
    }

    @MessageMapping("/driver/subscribe")
    public void subscribeToDriverUpdates(@Payload Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        String driverId = (String) message.get("driverId");

        logger.info("Driver {} subscribed to updates", driverId);

        headerAccessor.getSessionAttributes().put("driverId", driverId);
    }

    @SubscribeMapping("/topic/system-notifications")
    public void subscribeToSystemNotifications() {
        logger.debug("Client subscribed to system notifications");
    }

    @MessageMapping("/location/update")
    @SendTo("/topic/location-updates")
    public Map<String, Object> handleLocationUpdate(@Payload Map<String, Object> locationData) {
        logger.debug("Broadcasting location update via WebSocket");
        return locationData;
    }

    @MessageMapping("/status/update")
    @SendTo("/topic/status-updates")
    public Map<String, Object> handleStatusUpdate(@Payload Map<String, Object> statusData) {
        logger.debug("Broadcasting status update via WebSocket");
        return statusData;
    }
}