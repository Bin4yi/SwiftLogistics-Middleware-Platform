// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/NotificationService.java

package com.swiftlogistics.tracking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ============== MAIN NOTIFICATION METHODS ==============

    /**
     * Send status update notification (required by TrackingService)
     */
    public void sendStatusUpdateNotification(String orderNumber, String status, String clientId, String driverId) {
        logger.info("Sending status update notification: {} -> {} (client: {}, driver: {})",
                orderNumber, status, clientId, driverId);

        try {
            Map<String, Object> notification = createNotification(
                    "STATUS_UPDATE",
                    orderNumber,
                    status,
                    clientId,
                    driverId
            );

            // Send to multiple queues for different consumers
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.status.update", notification);
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);

            if (driverId != null) {
                rabbitTemplate.convertAndSend("tracking.exchange", "notification.driver." + driverId, notification);
            }

            // Cache notification
            cacheNotification("status:" + orderNumber, notification, 2);

            logger.debug("Status update notification sent successfully for order: {}", orderNumber);

        } catch (Exception e) {
            logger.error("Failed to send status update notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send order status notification
     */
    public void sendOrderStatusNotification(String orderNumber, String status, String clientId) {
        logger.info("Sending order status notification: {} -> {} (client: {})", orderNumber, status, clientId);

        try {
            Map<String, Object> notification = createNotification(
                    "ORDER_STATUS_UPDATE",
                    orderNumber,
                    status,
                    clientId,
                    null
            );

            // Send to notification queue
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.order.status", notification);
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);

            // Cache notification
            cacheNotification("order:" + orderNumber, notification, 1);

        } catch (Exception e) {
            logger.error("Failed to send order status notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send delivery completed notification
     */
    public void sendDeliveryCompletedNotification(String orderNumber, Map<String, Object> deliveryData) {
        logger.info("Sending delivery completed notification: {}", orderNumber);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "DELIVERY_COMPLETED");
            notification.put("orderNumber", orderNumber);
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("priority", "HIGH");
            notification.put("message", "Your order " + orderNumber + " has been successfully delivered!");

            if (deliveryData != null) {
                notification.put("deliveryData", deliveryData);
                notification.put("clientId", deliveryData.get("clientId"));
                notification.put("driverId", deliveryData.get("driverId"));
                notification.put("deliveryTime", deliveryData.get("deliveryTime"));
                notification.put("signature", deliveryData.get("signature"));
                notification.put("photo", deliveryData.get("photo"));
            }

            // Send notifications
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.delivery.completed", notification);

            String clientId = (String) deliveryData.get("clientId");
            if (clientId != null) {
                rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);
            }

            // Cache for client portal
            cacheNotification("completed:" + orderNumber, notification, 24);

        } catch (Exception e) {
            logger.error("Failed to send delivery completed notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send delivery failed notification
     */
    public void sendDeliveryFailedNotification(String orderNumber, String failureReason) {
        logger.warn("Sending delivery failed notification: {} - {}", orderNumber, failureReason);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "DELIVERY_FAILED");
            notification.put("orderNumber", orderNumber);
            notification.put("failureReason", failureReason != null ? failureReason : "Unknown reason");
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("priority", "URGENT");
            notification.put("message", "Delivery attempt failed for order " + orderNumber + ". Reason: " + failureReason);
            notification.put("actionRequired", true);
            notification.put("retryOptions", Map.of(
                    "reschedule", true,
                    "returnToWarehouse", true,
                    "contactCustomer", true
            ));

            // Send notifications
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.delivery.failed", notification);

            // Cache for immediate attention
            cacheNotification("failed:" + orderNumber, notification, 48);

        } catch (Exception e) {
            logger.error("Failed to send delivery failed notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send pickup completed notification
     */
    public void sendPickupCompletedNotification(String orderNumber, Map<String, Object> pickupData) {
        logger.info("Sending pickup completed notification: {}", orderNumber);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "PICKUP_COMPLETED");
            notification.put("orderNumber", orderNumber);
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("priority", "MEDIUM");
            notification.put("message", "Your order " + orderNumber + " has been picked up and is on its way!");

            if (pickupData != null) {
                notification.put("pickupData", pickupData);
                notification.put("driverId", pickupData.get("driverId"));
                notification.put("estimatedDelivery", pickupData.get("estimatedDelivery"));
                notification.put("clientId", pickupData.get("clientId"));
            }

            // Send notifications
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.pickup.completed", notification);

            String clientId = (String) (pickupData != null ? pickupData.get("clientId") : null);
            if (clientId != null) {
                rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);
            }

            // Cache notification
            cacheNotification("pickup:" + orderNumber, notification, 6);

        } catch (Exception e) {
            logger.error("Failed to send pickup completed notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send en route notification
     */
    public void sendEnRouteNotification(String orderNumber, Map<String, Object> routeData) {
        logger.info("Sending en route notification: {}", orderNumber);

        try {
            String driverId = (String) (routeData != null ? routeData.get("driverId") : null);
            String estimatedArrival = (String) (routeData != null ? routeData.get("estimatedArrival") : null);
            String clientId = (String) (routeData != null ? routeData.get("clientId") : null);

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "EN_ROUTE_TO_DELIVERY");
            notification.put("orderNumber", orderNumber);
            notification.put("driverId", driverId != null ? driverId : "Unknown");
            notification.put("estimatedArrival", estimatedArrival != null ? estimatedArrival : "Soon");
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("priority", "MEDIUM");
            notification.put("message", "Your order " + orderNumber + " is en route for delivery. Estimated arrival: " +
                    (estimatedArrival != null ? estimatedArrival : "Soon"));

            if (routeData != null) {
                notification.put("routeData", routeData);
                notification.put("currentLocation", routeData.get("currentLocation"));
                notification.put("nextStop", routeData.get("nextStop"));
                notification.put("clientId", clientId);
            }

            // Send notifications
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.enroute.delivery", notification);

            if (clientId != null) {
                rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);
            }

            if (driverId != null) {
                rabbitTemplate.convertAndSend("tracking.exchange", "notification.driver." + driverId, notification);
            }

            // Cache notification
            cacheNotification("enroute:" + orderNumber, notification, 4);

        } catch (Exception e) {
            logger.error("Failed to send en route notification for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Send driver notification
     */
    public void sendDriverNotification(String driverId, String message, String priority) {
        logger.info("Sending driver notification: {} - {}", driverId, message);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "DRIVER_NOTIFICATION");
            notification.put("driverId", driverId);
            notification.put("message", message);
            notification.put("priority", priority != null ? priority : "MEDIUM");
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("readStatus", false);
            notification.put("actionRequired", isActionRequired(message));

            // Send to driver-specific queue
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.driver." + driverId, notification);

            // Cache for driver mobile app
            String cacheKey = "notification:driver:" + driverId;
            redisTemplate.opsForList().leftPush(cacheKey, notification);
            redisTemplate.expire(cacheKey, 2, TimeUnit.HOURS);

        } catch (Exception e) {
            logger.error("Failed to send driver notification to {}: {}", driverId, e.getMessage(), e);
        }
    }

    /**
     * Send client notification
     */
    public void sendClientNotification(String clientId, String message, String type) {
        logger.info("Sending client notification: {} - {}", clientId, message);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type != null ? type : "CLIENT_NOTIFICATION");
            notification.put("clientId", clientId);
            notification.put("message", message);
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("priority", "MEDIUM");
            notification.put("readStatus", false);

            // Send to client-specific queue
            rabbitTemplate.convertAndSend("tracking.exchange", "notification.client." + clientId, notification);

            // Cache for client portal
            String cacheKey = "notification:client:" + clientId;
            redisTemplate.opsForList().leftPush(cacheKey, notification);
            redisTemplate.expire(cacheKey, 4, TimeUnit.HOURS);

        } catch (Exception e) {
            logger.error("Failed to send client notification to {}: {}", clientId, e.getMessage(), e);
        }
    }

    // ============== HELPER METHODS ==============

    /**
     * Create a standardized notification object
     */
    private Map<String, Object> createNotification(String type, String orderNumber, String status,
                                                   String clientId, String driverId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("orderNumber", orderNumber);
        notification.put("status", status);
        notification.put("clientId", clientId != null ? clientId : "UNKNOWN");
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("priority", getNotificationPriority(status));
        notification.put("message", generateStatusMessage(orderNumber, status));
        notification.put("readStatus", false);

        if (driverId != null) {
            notification.put("driverId", driverId);
        }

        return notification;
    }

    /**
     * Cache notification with TTL
     */
    private void cacheNotification(String suffix, Map<String, Object> notification, int hoursToLive) {
        try {
            String cacheKey = "notification:" + suffix;
            redisTemplate.opsForValue().set(cacheKey, notification, hoursToLive, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.warn("Failed to cache notification {}: {}", suffix, e.getMessage());
        }
    }

    /**
     * Get notification priority based on status
     */
    private String getNotificationPriority(String status) {
        if (status == null) return "LOW";

        switch (status.toUpperCase()) {
            case "DELIVERED":
                return "HIGH";
            case "FAILED":
            case "CANCELLED":
                return "URGENT";
            case "PICKED_UP":
            case "EN_ROUTE_DELIVERY":
            case "OUT_FOR_DELIVERY":
                return "MEDIUM";
            case "ASSIGNED":
            case "CONFIRMED":
                return "MEDIUM";
            case "CREATED":
            case "SUBMITTED":
                return "LOW";
            default:
                return "LOW";
        }
    }

    /**
     * Generate status-specific messages
     */
    private String generateStatusMessage(String orderNumber, String status) {
        if (status == null) return "Status update for order " + orderNumber;

        switch (status.toUpperCase()) {
            case "CREATED":
                return "Order " + orderNumber + " has been created successfully";
            case "CONFIRMED":
                return "Order " + orderNumber + " has been confirmed and is being processed";
            case "ASSIGNED":
                return "Order " + orderNumber + " has been assigned to a driver";
            case "PICKED_UP":
                return "Order " + orderNumber + " has been picked up from the warehouse";
            case "EN_ROUTE_PICKUP":
                return "Driver is on the way to pick up order " + orderNumber;
            case "EN_ROUTE_DELIVERY":
            case "IN_TRANSIT":
                return "Order " + orderNumber + " is on the way to you";
            case "OUT_FOR_DELIVERY":
                return "Order " + orderNumber + " is out for delivery";
            case "DELIVERED":
                return "Order " + orderNumber + " has been successfully delivered";
            case "FAILED":
                return "Delivery attempt for order " + orderNumber + " was unsuccessful";
            case "CANCELLED":
                return "Order " + orderNumber + " has been cancelled";
            default:
                return "Status update for order " + orderNumber + ": " + status;
        }
    }

    /**
     * Determine if message requires action
     */
    private boolean isActionRequired(String message) {
        if (message == null) return false;

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("failed") ||
                lowerMessage.contains("urgent") ||
                lowerMessage.contains("action required") ||
                lowerMessage.contains("retry") ||
                lowerMessage.contains("contact");
    }

    // ============== PUBLIC UTILITY METHODS ==============

    /**
     * Send custom notification
     */
    public void sendCustomNotification(String type, String recipient, String message, String priority) {
        logger.info("Sending custom notification: {} to {}", type, recipient);

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("recipient", recipient);
            notification.put("message", message);
            notification.put("priority", priority != null ? priority : "MEDIUM");
            notification.put("timestamp", LocalDateTime.now().toString());
            notification.put("readStatus", false);

            // Send to appropriate queue based on type
            String routingKey = type.toLowerCase().contains("driver") ?
                    "notification.driver." + recipient :
                    "notification.client." + recipient;

            rabbitTemplate.convertAndSend("tracking.exchange", routingKey, notification);

        } catch (Exception e) {
            logger.error("Failed to send custom notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Get cached notifications for a recipient
     */
    public Object getCachedNotifications(String recipientType, String recipientId) {
        try {
            String cacheKey = "notification:" + recipientType + ":" + recipientId;
            return redisTemplate.opsForList().range(cacheKey, 0, 10); // Get last 10 notifications
        } catch (Exception e) {
            logger.warn("Failed to get cached notifications for {} {}: {}", recipientType, recipientId, e.getMessage());
            return null;
        }
    }

    /**
     * Clear old notifications
     */
    public void clearOldNotifications(String recipientType, String recipientId) {
        try {
            String cacheKey = "notification:" + recipientType + ":" + recipientId;
            redisTemplate.delete(cacheKey);
            logger.debug("Cleared old notifications for {} {}", recipientType, recipientId);
        } catch (Exception e) {
            logger.warn("Failed to clear old notifications for {} {}: {}", recipientType, recipientId, e.getMessage());
        }
    }
}