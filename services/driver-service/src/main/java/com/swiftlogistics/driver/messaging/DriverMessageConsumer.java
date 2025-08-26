// services/driver-service/src/main/java/com/swiftlogistics/driver/messaging/DriverMessageConsumer.java
package com.swiftlogistics.driver.messaging;

import com.swiftlogistics.driver.entity.Delivery;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DeliveryStatus;
import com.swiftlogistics.driver.enums.OrderPriority;
import com.swiftlogistics.driver.repository.DeliveryRepository;
import com.swiftlogistics.driver.repository.DriverRepository;
import com.swiftlogistics.driver.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DriverMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DriverMessageConsumer.class);

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @RabbitListener(queues = "${rabbitmq.queues.driver-assignment:driver.assignment.queue}")
    @Transactional
    public void handleDriverAssignment(Map<String, Object> assignmentData) {
        String orderNumber = (String) assignmentData.get("orderNumber");
        String driverId = (String) assignmentData.get("driverId");

        logger.info("Received driver assignment: {} to {}", orderNumber, driverId);

        try {
            // Create delivery record if it doesn't exist
            if (!deliveryRepository.findByOrderNumber(orderNumber).isPresent()) {
                createDeliveryRecord(assignmentData);
            }

            // Assign delivery to driver
            deliveryService.assignDeliveryToDriver(orderNumber, driverId);

        } catch (Exception e) {
            logger.error("Error processing driver assignment: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.route-update:route.update.queue}")
    @Transactional
    public void handleRouteUpdate(Map<String, Object> routeData) {
        String routeId = (String) routeData.get("routeId");
        String driverId = (String) routeData.get("driverId");

        logger.info("Received route update for driver: {} route: {}", driverId, routeId);

        try {
            // Update deliveries with route information
            if (routeData.containsKey("deliveries")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> deliveries = (Map<String, Object>) routeData.get("deliveries");

                for (Map.Entry<String, Object> entry : deliveries.entrySet()) {
                    String orderNumber = entry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> deliveryInfo = (Map<String, Object>) entry.getValue();

                    updateDeliveryRouteInfo(orderNumber, routeId, deliveryInfo);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing route update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queues.emergency-stop:emergency.stop.queue}")
    @Transactional
    public void handleEmergencyStop(Map<String, Object> emergencyData) {
        String driverId = (String) emergencyData.get("driverId");
        String reason = (String) emergencyData.get("reason");

        logger.warn("Received emergency stop for driver: {} - {}", driverId, reason);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            // Suspend driver immediately
            driver.setActive(false);
            driver.updateStatus(com.swiftlogistics.driver.enums.DriverStatus.SUSPENDED);
            driverRepository.save(driver);

            // Cancel all active deliveries
            deliveryRepository.findActiveDeliveriesByDriver(driver)
                    .forEach(delivery -> {
                        delivery.updateStatus(DeliveryStatus.CANCELLED);
                        delivery.setFailureReason("Emergency stop: " + reason);
                        deliveryRepository.save(delivery);
                    });

            logger.info("Emergency stop processed for driver: {}", driverId);

        } catch (Exception e) {
            logger.error("Error processing emergency stop: {}", e.getMessage(), e);
        }
    }

    private void createDeliveryRecord(Map<String, Object> orderData) {
        String orderNumber = (String) orderData.get("orderNumber");
        String clientId = (String) orderData.get("clientId");
        String pickupAddress = (String) orderData.get("pickupAddress");
        String deliveryAddress = (String) orderData.get("deliveryAddress");
        String packageDescription = (String) orderData.get("packageDescription");
        String priorityStr = (String) orderData.get("priority");

        OrderPriority priority = OrderPriority.STANDARD;
        if (priorityStr != null) {
            try {
                priority = OrderPriority.valueOf(priorityStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid priority value: {}, using STANDARD", priorityStr);
            }
        }

        Delivery delivery = new Delivery(orderNumber, clientId, pickupAddress,
                deliveryAddress, packageDescription, priority);

        // Set additional fields if available
        if (orderData.containsKey("recipientName")) {
            delivery.setRecipientName((String) orderData.get("recipientName"));
        }
        if (orderData.containsKey("recipientPhone")) {
            delivery.setRecipientPhone((String) orderData.get("recipientPhone"));
        }
        if (orderData.containsKey("specialInstructions")) {
            delivery.setSpecialInstructions((String) orderData.get("specialInstructions"));
        }
        if (orderData.containsKey("scheduledDate")) {
            try {
                delivery.setScheduledDate(LocalDateTime.parse((String) orderData.get("scheduledDate")));
            } catch (Exception e) {
                logger.warn("Invalid scheduled date format: {}", orderData.get("scheduledDate"));
            }
        }

        deliveryRepository.save(delivery);
        logger.info("Created delivery record: {}", orderNumber);
    }

    private void updateDeliveryRouteInfo(String orderNumber, String routeId, Map<String, Object> deliveryInfo) {
        deliveryRepository.findByOrderNumber(orderNumber).ifPresent(delivery -> {
            delivery.setRouteId(routeId);

            if (deliveryInfo.containsKey("sequence")) {
                delivery.setRouteSequence((Integer) deliveryInfo.get("sequence"));
            }

            if (deliveryInfo.containsKey("estimatedPickupTime")) {
                try {
                    delivery.setScheduledDate(LocalDateTime.parse((String) deliveryInfo.get("estimatedPickupTime")));
                } catch (Exception e) {
                    logger.warn("Invalid estimated pickup time format: {}", deliveryInfo.get("estimatedPickupTime"));
                }
            }

            deliveryRepository.save(delivery);
            logger.debug("Updated route info for delivery: {}", orderNumber);
        });
    }
}