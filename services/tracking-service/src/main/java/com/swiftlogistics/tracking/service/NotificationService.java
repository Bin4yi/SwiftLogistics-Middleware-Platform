// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/NotificationService.java

package com.swiftlogistics.tracking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void sendOrderStatusNotification(String orderNumber, String status, String clientId) {
        logger.info("Sending order status notification: {} -> {}", orderNumber, status);

        Map<String, Object> notification = Map.of(
                "type", "ORDER_STATUS_UPDATE",
                "orderNumber", orderNumber,
                "status", status,
                "clientId", clientId != null ? clientId : "UNKNOWN",
                "timestamp", LocalDateTime.now().toString(),
                "priority", getNotificationPriority(status),
                "message", generateStatusMessage(orderNumber, status)
        );

        // Send to notification queue
        rabbitTemplate.convertAndSend("tracking.exchange", "notification.order.status", notification);

        // Cache notification for quick retrieval
        String cacheKey = "notification:order:" + orderNumber;
        redisTemplate.opsForValue().set(cacheKey, notification, 1, TimeUnit.HOURS);
    }

    public void sendDeliveryCompletedNotification(String orderNumber, Map<String, Object> deliveryData) {
        logger.info("Sending delivery completed notification: {}", orderNumber);

        Map<String, Object> notification = Map.of(
                "type", "DELIVERY_COMPLETED",
                "orderNumber", orderNumber,
                "timestamp", LocalDateTime.now().toString(),
                "priority", "HIGH",
                "message", "Your order " + orderNumber + " has been successfully delivered!",
                "deliveryData", deliveryData
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.delivery.completed", notification);

        // Cache for client portal
        String cacheKey = "notification:completed:" + orderNumber;
        redisTemplate.opsForValue().set(cacheKey, notification, 24, TimeUnit.HOURS);
    }

    public void sendDeliveryFailedNotification(String orderNumber, String failureReason) {
        logger.warn("Sending delivery failed notification: {} - {}", orderNumber, failureReason);

        Map<String, Object> notification = Map.of(
                "type", "DELIVERY_FAILED",
                "orderNumber", orderNumber,
                "failureReason", failureReason != null ? failureReason : "Unknown reason",
                "timestamp", LocalDateTime.now().toString(),
                "priority", "URGENT",
                "message", "Delivery attempt failed for order " + orderNumber + ". Reason: " + failureReason,
                "actionRequired", true
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.delivery.failed", notification);

        // Cache for immediate attention
        String cacheKey = "notification:failed:" + orderNumber;
        redisTemplate.opsForValue().set(cacheKey, notification, 48, TimeUnit.HOURS);
    }

    public void sendPickupCompletedNotification(String orderNumber, Map<String, Object> pickupData) {
        logger.info("Sending pickup completed notification: {}", orderNumber);

        Map<String, Object> notification = Map.of(
                "type", "PICKUP_COMPLETED",
                "orderNumber", orderNumber,
                "timestamp", LocalDateTime.now().toString(),
                "priority", "MEDIUM",
                "message", "Your order " + orderNumber + " has been picked up and is on its way!",
                "pickupData", pickupData
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.pickup.completed", notification);
    }

    public void sendEnRouteNotification(String orderNumber, Map<String, Object> routeData) {
        logger.info("Sending en route notification: {}", orderNumber);

        String driverId = (String) routeData.get("driverId");
        String estimatedArrival = (String) routeData.get("estimatedArrival");

        Map<String, Object> notification = Map.of(
                "type", "EN_ROUTE_TO_DELIVERY",
                "orderNumber", orderNumber,
                "driverId", driverId != null ? driverId : "Unknown",
                "estimatedArrival", estimatedArrival != null ? estimatedArrival : "Soon",
                "timestamp", LocalDateTime.now().toString(),
                "priority", "MEDIUM",
                "message", "Your order " + orderNumber + " is en route for delivery. Estimated arrival: " + estimatedArrival
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.enroute.delivery", notification);
    }

    public void sendDriverNotification(String driverId, String message, String priority) {
        logger.info("Sending driver notification: {} - {}", driverId, message);

        Map<String, Object> notification = Map.of(
                "type", "DRIVER_NOTIFICATION",
                "driverId", driverId,
                "message", message,
                "priority", priority != null ? priority : "MEDIUM",
                "timestamp", LocalDateTime.now().toString()
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.driver." + driverId, notification);

        // Cache for driver mobile app
        String cacheKey = "notification:driver:" + driverId;
        redisTemplate.opsForList().leftPush(cacheKey, notification);
        redisTemplate.expire(cacheKey, 2, TimeUnit.HOURS);
    }

    public void sendClientNotification(String clientId, String message, String type) {
        logger.info("Sending client notification: {} - {}", clientId, message);

        Map<String, Object> notification = Map.of(
                "type", type != null ? type : "CLIENT_NOTIFICATION",
                "clientId", clientId,
                "message", message,
                "timestamp", LocalDateTime.now().toString(),
                "priority", "MEDIUM"
        );

        rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);

        // Cache for client portal
        String cacheKey = "notification:client:" + clientId;
        redisTemplate.opsForList().leftPush(cacheKey, notification);
        redisTemplate.expire(cacheKey, 4, TimeUnit.HOURS);
    }

    private String getNotificationPriority(String status) {
        switch (status.toUpperCase()) {
            case "DELIVERED":
                return "HIGH";
            case "FAILED":
                return "URGENT";
            case "PICKED_UP":
            case "EN_ROUTE_DELIVERY":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }

    private String generateStatusMessage(String orderNumber, String status) {
        switch (status.toUpperCase()) {
            case "ASSIGNED":
                return "Order " + orderNumber + " has been assigned to a driver";
            case "PICKED_UP":
                return "Order " + orderNumber + " has been picked up from the warehouse";
            case "EN_ROUTE_PICKUP":
                return "Driver is on the way to pick up order " + orderNumber;
            case "EN_ROUTE_DELIVERY":
                return "Order " + orderNumber + " is on the way to you";
            case "DELIVERED":
                return "Order " + orderNumber + " has been successfully delivered";
            case "FAILED":
                return "Delivery attempt for order " + orderNumber + " was unsuccessful";
            default:
                return "Status update for order " + orderNumber + ": " + status;
        }
    }
}