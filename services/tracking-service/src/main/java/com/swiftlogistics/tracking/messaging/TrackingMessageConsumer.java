// services/tracking-service/src/main/java/com/swiftlogistics/tracking/messaging/TrackingMessageConsumer.java
package com.swiftlogistics.tracking.messaging;

import com.swiftlogistics.tracking.service.NotificationService;
import com.swiftlogistics.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TrackingMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TrackingMessageConsumer.class);

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queues.order-status:order.status.queue}")
    public void handleOrderStatusUpdate(Map<String, Object> message) {
        try {
            String orderNumber = (String) message.get("orderNumber");
            String newStatus = (String) message.get("newStatus");
            String oldStatus = (String) message.get("oldStatus");
            String driverId = (String) message.get("driverId");
            String clientId = (String) message.get("clientId");

            logger.info("Processing order status update: {} -> {}", oldStatus, newStatus);

            // Handle new order creation
            if ("ORDER_CREATED".equals(message.get("eventType"))) {
                trackingService.createOrderTracking(orderNumber, clientId, newStatus);
            } else {
                // Update existing order status
                trackingService.updateOrderStatus(orderNumber, newStatus, driverId, null);
            }

            // Send notification to client
            if (clientId != null) {
                notificationService.sendOrderStatusUpdate(orderNumber, clientId, oldStatus, newStatus);
            }

        } catch (Exception e) {
            logger.error("Error processing order status update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.driver-location:driver.location.queue}")
    public void handleDriverLocationUpdate(Map<String, Object> message) {
        try {
            String driverId = (String) message.get("driverId");
            Double latitude = (Double) message.get("latitude");
            Double longitude = (Double) message.get("longitude");
            Double speed = (Double) message.get("speed");
            Integer heading = (Integer) message.get("heading");
            Double accuracy = (Double) message.get("accuracy");

            logger.debug("Processing driver location update: {} at ({}, {})", driverId, latitude, longitude);

            trackingService.updateDriverLocation(driverId, latitude, longitude, speed, heading, accuracy);

        } catch (Exception e) {
            logger.error("Error processing driver location update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.delivery-status:delivery.status.queue}")
    public void handleDeliveryStatusUpdate(Map<String, Object> message) {
        try {
            String orderNumber = (String) message.get("orderNumber");
            String driverId = (String) message.get("driverId");
            String newStatus = (String) message.get("newStatus");
            String oldStatus = (String) message.get("oldStatus");
            String notes = (String) message.get("notes");
            String clientId = (String) message.get("clientId");

            logger.info("Processing delivery status update: {} -> {}", oldStatus, newStatus);

            String description = notes != null ? notes : String.format("Delivery status updated to %s", newStatus);
            trackingService.updateOrderStatus(orderNumber, newStatus, driverId, description);

            // Send notification to client
            if (clientId != null) {
                notificationService.sendDeliveryStatusUpdate(orderNumber, clientId, driverId, newStatus, description);
            }

        } catch (Exception e) {
            logger.error("Error processing delivery status update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "driver.assignment.queue")
    public void handleDriverAssignment(Map<String, Object> message) {
        try {
            String orderNumber = (String) message.get("orderNumber");
            String driverId = (String) message.get("driverId");

            logger.info("Processing driver assignment: {} -> {}", orderNumber, driverId);

            trackingService.assignDriverToOrder(orderNumber, driverId);

        } catch (Exception e) {
            logger.error("Error processing driver assignment: {}", e.getMessage(), e);
        }
    }
}
