// services/tracking-service/src/main/java/com/swiftlogistics/tracking/messaging/TrackingMessageProducer.java
package com.swiftlogistics.tracking.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TrackingMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(TrackingMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.tracking:tracking.exchange}")
    private String trackingExchange;

    public void sendTrackingUpdate(String orderNumber, String status, String driverId, Double latitude, Double longitude) {
        logger.debug("Sending tracking update: {} - {}", orderNumber, status);

        Map<String, Object> message = new HashMap<>();
        message.put("orderNumber", orderNumber);
        message.put("status", status);
        message.put("driverId", driverId);
        message.put("latitude", latitude);
        message.put("longitude", longitude);
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(trackingExchange, "tracking.update", message);
            logger.debug("Tracking update sent: {}", orderNumber);
        } catch (Exception e) {
            logger.error("Failed to send tracking update: {}", e.getMessage(), e);
        }
    }

    public void sendLocationUpdate(String driverId, Double latitude, Double longitude) {
        logger.debug("Sending location update: {} at ({}, {})", driverId, latitude, longitude);

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driverId);
        message.put("latitude", latitude);
        message.put("longitude", longitude);
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(trackingExchange, "location.update", message);
            logger.debug("Location update sent for driver: {}", driverId);
        } catch (Exception e) {
            logger.error("Failed to send location update: {}", e.getMessage(), e);
        }
    }

    public void sendNotificationRequest(String type, String recipient, String title, String message, Map<String, Object> data) {
        logger.debug("Sending notification request: {} to {}", type, recipient);

        Map<String, Object> notificationMessage = new HashMap<>();
        notificationMessage.put("type", type);
        notificationMessage.put("recipient", recipient);
        notificationMessage.put("title", title);
        notificationMessage.put("message", message);
        notificationMessage.put("data", data);
        notificationMessage.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(trackingExchange, "notification.request", notificationMessage);
            logger.debug("Notification request sent: {} to {}", type, recipient);
        } catch (Exception e) {
            logger.error("Failed to send notification request: {}", e.getMessage(), e);
        }
    }
}