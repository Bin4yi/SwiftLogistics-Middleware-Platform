// services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/NotificationService.java
package com.swiftlogistics.tracking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendOrderStatusUpdate(String orderNumber, String clientId, String oldStatus, String newStatus) {
        logger.info("Notification: Order {} status changed from {} to {} for client {}",
                orderNumber, oldStatus, newStatus, clientId);
        // TODO: Implement actual notification when RabbitMQ is enabled
    }

    public void sendDriverAssignmentNotification(String orderNumber, String driverId) {
        logger.info("Notification: Driver {} assigned to order {}", driverId, orderNumber);
        // TODO: Implement actual notification when RabbitMQ is enabled
    }

    public void sendLocationUpdate(String orderNumber, String clientId, Double latitude, Double longitude) {
        logger.info("Notification: Location update for order {} - ({}, {})", orderNumber, latitude, longitude);
        // TODO: Implement actual notification when RabbitMQ is enabled
    }
}