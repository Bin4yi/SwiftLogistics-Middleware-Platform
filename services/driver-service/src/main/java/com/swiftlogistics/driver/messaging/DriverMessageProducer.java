// services/driver-service/src/main/java/com/swiftlogistics/driver/service/messaging/DriverMessageProducer.java
package com.swiftlogistics.driver.service.messaging;

import com.swiftlogistics.driver.entity.Delivery;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DeliveryStatus;
import com.swiftlogistics.driver.enums.DriverStatus;
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
public class DriverMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(DriverMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.driver:driver.exchange}")
    private String driverExchange;

    @Value("${rabbitmq.exchanges.tracking:tracking.exchange}")
    private String trackingExchange;

    public void sendDriverRegistered(Driver driver) {
        logger.info("Sending driver registration notification: {}", driver.getDriverId());

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driver.getDriverId());
        message.put("fullName", driver.getFullName());
        message.put("email", driver.getEmail());
        message.put("phoneNumber", driver.getPhoneNumber());
        message.put("vehicleType", driver.getVehicleType().toString());
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(driverExchange, "driver.registered", message);
            logger.debug("Driver registration notification sent: {}", driver.getDriverId());
        } catch (Exception e) {
            logger.error("Failed to send driver registration notification: {}", e.getMessage(), e);
        }
    }

    public void sendDriverStatusUpdate(Driver driver, DriverStatus oldStatus, DriverStatus newStatus) {
        logger.debug("Sending driver status update: {} from {} to {}",
                driver.getDriverId(), oldStatus, newStatus);

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driver.getDriverId());
        message.put("oldStatus", oldStatus != null ? oldStatus.toString() : null);
        message.put("newStatus", newStatus.toString());
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(driverExchange, "driver.status.changed", message);

            // Also send to tracking service for real-time updates
            rabbitTemplate.convertAndSend(trackingExchange, "driver.status.update", message);

            logger.debug("Driver status update sent: {}", driver.getDriverId());
        } catch (Exception e) {
            logger.error("Failed to send driver status update: {}", e.getMessage(), e);
        }
    }

    public void sendDriverLocationUpdate(Driver driver) {
        logger.debug("Sending driver location update: {}", driver.getDriverId());

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driver.getDriverId());
        message.put("latitude", driver.getCurrentLatitude());
        message.put("longitude", driver.getCurrentLongitude());
        message.put("timestamp", driver.getLastLocationUpdate() != null ?
                driver.getLastLocationUpdate().toString() : LocalDateTime.now().toString());

        try {
            // Send to tracking service for real-time location updates
            rabbitTemplate.convertAndSend(trackingExchange, "driver.location.update", message);
            logger.debug("Driver location update sent: {}", driver.getDriverId());
        } catch (Exception e) {
            logger.error("Failed to send driver location update: {}", e.getMessage(), e);
        }
    }

    public void sendDeliveryStatusUpdate(Delivery delivery, DeliveryStatus oldStatus, DeliveryStatus newStatus) {
        logger.info("Sending delivery status update: {} from {} to {}",
                delivery.getOrderNumber(), oldStatus, newStatus);

        Map<String, Object> message = new HashMap<>();
        message.put("orderNumber", delivery.getOrderNumber());
        message.put("driverId", delivery.getDriver().getDriverId());
        message.put("oldStatus", oldStatus != null ? oldStatus.toString() : null);
        message.put("newStatus", newStatus.toString());
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("notes", delivery.getDeliveryNotes());
        message.put("proofOfDelivery", delivery.getProofOfDeliveryPhoto());
        message.put("failureReason", delivery.getFailureReason());

        // Add location data if available
        if (newStatus == DeliveryStatus.PICKED_UP && delivery.getPickupLatitude() != null) {
            message.put("latitude", delivery.getPickupLatitude());
            message.put("longitude", delivery.getPickupLongitude());
        } else if (newStatus == DeliveryStatus.DELIVERED && delivery.getDeliveryLatitude() != null) {
            message.put("latitude", delivery.getDeliveryLatitude());
            message.put("longitude", delivery.getDeliveryLongitude());
        }

        try {
            // Send to order service to update order status
            rabbitTemplate.convertAndSend("order.exchange", "order.delivery.status.update", message);

            // Send to tracking service for real-time updates
            rabbitTemplate.convertAndSend(trackingExchange, "delivery.status.update", message);

            logger.info("Delivery status update sent: {}", delivery.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to send delivery status update: {}", e.getMessage(), e);
        }
    }

    public void sendDeliveryAssigned(Delivery delivery) {
        logger.info("Sending delivery assignment notification: {} to {}",
                delivery.getOrderNumber(), delivery.getDriver().getDriverId());

        Map<String, Object> message = new HashMap<>();
        message.put("orderNumber", delivery.getOrderNumber());
        message.put("driverId", delivery.getDriver().getDriverId());
        message.put("pickupAddress", delivery.getPickupAddress());
        message.put("deliveryAddress", delivery.getDeliveryAddress());
        message.put("priority", delivery.getPriority().toString());
        message.put("scheduledDate", delivery.getScheduledDate() != null ?
                delivery.getScheduledDate().toString() : null);
        message.put("assignedAt", delivery.getAssignedAt().toString());

        try {
            rabbitTemplate.convertAndSend(driverExchange, "delivery.assigned", message);

            // Send to tracking service
            rabbitTemplate.convertAndSend(trackingExchange, "delivery.assigned", message);

            logger.info("Delivery assignment notification sent: {}", delivery.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to send delivery assignment notification: {}", e.getMessage(), e);
        }
    }

    public void sendDriverVerified(Driver driver) {
        logger.info("Sending driver verification notification: {}", driver.getDriverId());

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driver.getDriverId());
        message.put("fullName", driver.getFullName());
        message.put("email", driver.getEmail());
        message.put("verified", true);
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(driverExchange, "driver.verified", message);
            logger.info("Driver verification notification sent: {}", driver.getDriverId());
        } catch (Exception e) {
            logger.error("Failed to send driver verification notification: {}", e.getMessage(), e);
        }
    }

    public void sendDriverSuspended(Driver driver, String reason) {
        logger.info("Sending driver suspension notification: {} - {}", driver.getDriverId(), reason);

        Map<String, Object> message = new HashMap<>();
        message.put("driverId", driver.getDriverId());
        message.put("fullName", driver.getFullName());
        message.put("email", driver.getEmail());
        message.put("suspensionReason", reason);
        message.put("timestamp", LocalDateTime.now().toString());

        try {
            rabbitTemplate.convertAndSend(driverExchange, "driver.suspended", message);
            logger.info("Driver suspension notification sent: {}", driver.getDriverId());
        } catch (Exception e) {
            logger.error("Failed to send driver suspension notification: {}", e.getMessage(), e);
        }
    }
}