// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/RealTimeTrackingService.java
// UPDATED VERSION using your existing TrackingEventType enum

package com.swiftlogistics.tracking.service;

import com.swiftlogistics.tracking.dto.TrackingUpdateMessage;
import com.swiftlogistics.tracking.entity.DeliveryTracking;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.enums.TrackingEventType;
import com.swiftlogistics.tracking.repository.DeliveryTrackingRepository;
import com.swiftlogistics.tracking.repository.TrackingEventRepository;
import com.swiftlogistics.tracking.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RealTimeTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeTrackingService.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    // ============== RABBIT MQ MESSAGE HANDLERS ==============

    @RabbitListener(queues = "tracking.order.updates")
    @Transactional
    public void handleOrderStatusUpdate(Map<String, Object> message) {
        logger.info("Received order status update: {}", message);

        try {
            String orderNumber = (String) message.get("orderNumber");
            String newStatus = (String) message.get("newStatus");
            String clientId = (String) message.get("clientId");

            if (orderNumber == null || newStatus == null) {
                logger.warn("Invalid order status update message: missing orderNumber or newStatus");
                return;
            }

            // Update tracking in database
            updateOrderTrackingStatus(orderNumber, newStatus, clientId, message);

            // Create appropriate tracking event based on status
            TrackingEventType eventType = mapStatusToEventType(newStatus);
            createTrackingEvent(orderNumber, eventType,
                    eventType.getDescription() + ": " + newStatus, message);

            // Cache the update for quick retrieval
            cacheTrackingUpdate(orderNumber, message);

            // Broadcast real-time update to WebSocket clients
            sessionManager.broadcastOrderUpdate(orderNumber, message);

            // Send notifications if needed
            if (shouldSendNotification(newStatus)) {
                notificationService.sendOrderStatusNotification(orderNumber, newStatus, clientId);
            }

            logger.debug("Successfully processed order status update for: {}", orderNumber);

        } catch (Exception e) {
            logger.error("Error processing order status update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "tracking.driver.location")
    @Transactional
    public void handleDriverLocationUpdate(Map<String, Object> message) {
        logger.debug("Received driver location update: {}", message);

        try {
            String driverId = (String) message.get("driverId");
            Double latitude = getDoubleFromMessage(message, "latitude");
            Double longitude = getDoubleFromMessage(message, "longitude");

            if (driverId == null || latitude == null || longitude == null) {
                logger.warn("Invalid driver location update: missing required fields");
                return;
            }

            // Update driver location in cache (fast access)
            cacheDriverLocation(driverId, latitude, longitude, message);

            // Update any active deliveries for this driver
            updateActiveDeliveryLocations(driverId, latitude, longitude);

            // Create location update event for active orders
            createLocationEventForActiveOrders(driverId, latitude, longitude, message);

            // Broadcast real-time location update
            sessionManager.broadcastDriverUpdate(driverId, message);

            logger.debug("Successfully processed driver location update for: {}", driverId);

        } catch (Exception e) {
            logger.error("Error processing driver location update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "tracking.delivery.status")
    @Transactional
    public void handleDeliveryStatusUpdate(Map<String, Object> message) {
        logger.info("Received delivery status update: {}", message);

        try {
            String orderNumber = (String) message.get("orderNumber");
            String driverId = (String) message.get("driverId");
            String newStatus = (String) message.get("newStatus");
            String oldStatus = (String) message.get("oldStatus");

            if (orderNumber == null || newStatus == null) {
                logger.warn("Invalid delivery status update: missing required fields");
                return;
            }

            // Update delivery tracking
            updateDeliveryStatus(orderNumber, driverId, newStatus, oldStatus, message);

            // Create appropriate tracking event
            TrackingEventType eventType = mapDeliveryStatusToEventType(newStatus, oldStatus);
            String eventDescription = String.format("Delivery status changed from %s to %s",
                    oldStatus != null ? oldStatus : "Unknown", newStatus);
            createTrackingEvent(orderNumber, eventType, eventDescription, message);

            // Handle special delivery events
            handleSpecialDeliveryEvents(orderNumber, newStatus, message);

            // Cache the update
            cacheTrackingUpdate(orderNumber, message);

            // Broadcast to all relevant subscribers
            sessionManager.broadcastOrderUpdate(orderNumber, message);
            if (driverId != null) {
                sessionManager.broadcastDriverUpdate(driverId, message);
            }

            logger.info("Successfully processed delivery status update: {} -> {}", orderNumber, newStatus);

        } catch (Exception e) {
            logger.error("Error processing delivery status update: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "tracking.integration.updates")
    @Transactional
    public void handleIntegrationUpdate(Map<String, Object> message) {
        logger.info("Received integration update: {}", message);

        try {
            String orderNumber = (String) message.get("orderNumber");
            String source = (String) message.get("source"); // CMS, ROS, WMS
            String status = (String) message.get("status");

            if (orderNumber == null || source == null) {
                logger.warn("Invalid integration update: missing required fields");
                return;
            }

            // Create integration tracking event
            String eventDescription = String.format("%s integration: %s", source, status);
            createTrackingEvent(orderNumber, TrackingEventType.STATUS_UPDATE, eventDescription, message);

            // Update integration status in tracking
            updateIntegrationStatus(orderNumber, source, status, message);

            // Broadcast update
            sessionManager.broadcastOrderUpdate(orderNumber, message);

            logger.debug("Successfully processed integration update: {} from {}", orderNumber, source);

        } catch (Exception e) {
            logger.error("Error processing integration update: {}", e.getMessage(), e);
        }
    }

    // ============== HELPER METHODS ==============

    private TrackingEventType mapStatusToEventType(String status) {
        switch (status.toUpperCase()) {
            case "CREATED":
            case "SUBMITTED":
                return TrackingEventType.ORDER_CREATED;
            case "CONFIRMED":
                return TrackingEventType.ORDER_CONFIRMED;
            case "ASSIGNED":
                return TrackingEventType.ORDER_ASSIGNED_TO_DRIVER;
            case "PICKED_UP":
                return TrackingEventType.ORDER_PICKED_UP;
            case "IN_TRANSIT":
            case "EN_ROUTE_DELIVERY":
                return TrackingEventType.ORDER_IN_TRANSIT;
            case "OUT_FOR_DELIVERY":
                return TrackingEventType.ORDER_OUT_FOR_DELIVERY;
            case "DELIVERED":
                return TrackingEventType.ORDER_DELIVERED;
            case "FAILED":
                return TrackingEventType.ORDER_FAILED;
            case "CANCELLED":
                return TrackingEventType.ORDER_CANCELLED;
            default:
                return TrackingEventType.STATUS_UPDATE;
        }
    }

    private TrackingEventType mapDeliveryStatusToEventType(String newStatus, String oldStatus) {
        switch (newStatus.toUpperCase()) {
            case "ASSIGNED":
                return TrackingEventType.DRIVER_ASSIGNED;
            case "EN_ROUTE_PICKUP":
            case "EN_ROUTE_DELIVERY":
                return TrackingEventType.DRIVER_EN_ROUTE;
            case "AT_PICKUP":
            case "AT_DELIVERY":
                return TrackingEventType.DRIVER_ARRIVED;
            case "PICKED_UP":
                return TrackingEventType.ORDER_PICKED_UP;
            case "DELIVERED":
                return TrackingEventType.ORDER_DELIVERED;
            case "FAILED":
                return TrackingEventType.ORDER_FAILED;
            default:
                return TrackingEventType.STATUS_UPDATE;
        }
    }

    private void createLocationEventForActiveOrders(String driverId, Double latitude, Double longitude, Map<String, Object> message) {
        // Find active orders for this driver and create location update events
        deliveryTrackingRepository.findByAssignedDriverIdAndCurrentStatusIn(
                driverId,
                java.util.List.of("ASSIGNED", "PICKED_UP", "EN_ROUTE_PICKUP", "EN_ROUTE_DELIVERY")
        ).forEach(tracking -> {
            createTrackingEvent(tracking.getOrderNumber(), TrackingEventType.LOCATION_UPDATE,
                    "Driver location updated", message);
        });
    }

    private void updateOrderTrackingStatus(String orderNumber, String newStatus, String clientId, Map<String, Object> message) {
        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);

        DeliveryTracking tracking;
        if (trackingOpt.isPresent()) {
            tracking = trackingOpt.get();
            tracking.setCurrentStatus(newStatus);
            tracking.setUpdatedAt(LocalDateTime.now());

            // Update location if provided
            Double latitude = getDoubleFromMessage(message, "latitude");
            Double longitude = getDoubleFromMessage(message, "longitude");
            if (latitude != null && longitude != null) {
                tracking.setLastKnownLatitude(latitude);
                tracking.setLastKnownLongitude(longitude);
                tracking.setLastLocationUpdate(LocalDateTime.now());
            }

        } else {
            // Create new tracking record
            tracking = new DeliveryTracking();
            tracking.setOrderNumber(orderNumber);
            tracking.setClientId(clientId != null ? clientId : "UNKNOWN");
            tracking.setCurrentStatus(newStatus);
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());
        }

        deliveryTrackingRepository.save(tracking);
    }

    private void updateDeliveryStatus(String orderNumber, String driverId, String newStatus, String oldStatus, Map<String, Object> message) {
        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);

        if (trackingOpt.isPresent()) {
            DeliveryTracking tracking = trackingOpt.get();
            tracking.setCurrentStatus(newStatus);
            tracking.setUpdatedAt(LocalDateTime.now());

            if (driverId != null) {
                tracking.setAssignedDriverId(driverId);
            }

            // Update location data if provided
            Double latitude = getDoubleFromMessage(message, "latitude");
            Double longitude = getDoubleFromMessage(message, "longitude");
            if (latitude != null && longitude != null) {
                tracking.setLastKnownLatitude(latitude);
                tracking.setLastKnownLongitude(longitude);
                tracking.setLastLocationUpdate(LocalDateTime.now());
            }

            deliveryTrackingRepository.save(tracking);
        }
    }

    private void updateIntegrationStatus(String orderNumber, String source, String status, Map<String, Object> message) {
        // Update integration status in tracking metadata
        String cacheKey = "integration:" + orderNumber + ":" + source;
        redisTemplate.opsForValue().set(cacheKey, status, 1, TimeUnit.HOURS);
    }

    private void updateActiveDeliveryLocations(String driverId, Double latitude, Double longitude) {
        // Update location for all active deliveries assigned to this driver
        deliveryTrackingRepository.findByAssignedDriverIdAndCurrentStatusIn(
                driverId,
                java.util.List.of("ASSIGNED", "PICKED_UP", "EN_ROUTE_PICKUP", "EN_ROUTE_DELIVERY")
        ).forEach(tracking -> {
            tracking.setLastKnownLatitude(latitude);
            tracking.setLastKnownLongitude(longitude);
            tracking.setLastLocationUpdate(LocalDateTime.now());
            deliveryTrackingRepository.save(tracking);
        });
    }

    private void createTrackingEvent(String orderNumber, TrackingEventType eventType, String description, Map<String, Object> message) {
        TrackingEvent event = new TrackingEvent();
        event.setOrderNumber(orderNumber);
        event.setEventType(eventType);
        event.setEventDescription(description);
        event.setTimestamp(LocalDateTime.now());

        // Add driver info if available
        String driverId = (String) message.get("driverId");
        if (driverId != null) {
            event.setDriverId(driverId);
        }

        // Add location info if available
        Double latitude = getDoubleFromMessage(message, "latitude");
        Double longitude = getDoubleFromMessage(message, "longitude");
        if (latitude != null && longitude != null) {
            event.setLatitude(latitude);
            event.setLongitude(longitude);
        }

        // Store additional metadata as JSON
        try {
            String metadata = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(message);
            event.setMetadata(metadata);
        } catch (Exception e) {
            logger.warn("Failed to serialize metadata for tracking event: {}", e.getMessage());
        }

        trackingEventRepository.save(event);
    }

    private void handleSpecialDeliveryEvents(String orderNumber, String newStatus, Map<String, Object> message) {
        switch (newStatus.toUpperCase()) {
            case "DELIVERED":
                handleDeliveryCompleted(orderNumber, message);
                break;
            case "FAILED":
                handleDeliveryFailed(orderNumber, message);
                break;
            case "PICKED_UP":
                handlePickupCompleted(orderNumber, message);
                break;
            case "EN_ROUTE_DELIVERY":
                handleEnRouteToDelivery(orderNumber, message);
                break;
            case "ASSIGNED":
                handleDriverAssigned(orderNumber, message);
                break;
        }
    }

    private void handleDeliveryCompleted(String orderNumber, Map<String, Object> message) {
        logger.info("Delivery completed for order: {}", orderNumber);

        // Create completion event
        createTrackingEvent(orderNumber, TrackingEventType.ORDER_DELIVERED,
                "Order successfully delivered", message);

        // Update estimated delivery time to actual delivery time
        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
        if (trackingOpt.isPresent()) {
            DeliveryTracking tracking = trackingOpt.get();
            tracking.setEstimatedDeliveryTime(LocalDateTime.now()); // Set to actual delivery time
            deliveryTrackingRepository.save(tracking);
        }

        // Send completion notification
        notificationService.sendDeliveryCompletedNotification(orderNumber, message);
    }

    private void handleDeliveryFailed(String orderNumber, Map<String, Object> message) {
        logger.warn("Delivery failed for order: {}", orderNumber);

        // Create failure event
        String failureReason = (String) message.get("failureReason");
        createTrackingEvent(orderNumber, TrackingEventType.ORDER_FAILED,
                "Delivery failed: " + (failureReason != null ? failureReason : "Unknown reason"), message);

        notificationService.sendDeliveryFailedNotification(orderNumber, failureReason);
    }

    private void handlePickupCompleted(String orderNumber, Map<String, Object> message) {
        logger.info("Pickup completed for order: {}", orderNumber);

        // Create pickup event
        createTrackingEvent(orderNumber, TrackingEventType.ORDER_PICKED_UP,
                "Order picked up from warehouse", message);

        notificationService.sendPickupCompletedNotification(orderNumber, message);
    }

    private void handleEnRouteToDelivery(String orderNumber, Map<String, Object> message) {
        logger.info("Order en route to delivery: {}", orderNumber);

        // Create en route event
        createTrackingEvent(orderNumber, TrackingEventType.ORDER_IN_TRANSIT,
                "Order is en route to delivery location", message);

        notificationService.sendEnRouteNotification(orderNumber, message);
    }

    private void handleDriverAssigned(String orderNumber, Map<String, Object> message) {
        logger.info("Driver assigned to order: {}", orderNumber);

        String driverId = (String) message.get("driverId");
        createTrackingEvent(orderNumber, TrackingEventType.DRIVER_ASSIGNED,
                "Driver " + driverId + " assigned to order", message);
    }

    private void cacheTrackingUpdate(String orderNumber, Map<String, Object> message) {
        String cacheKey = "tracking:" + orderNumber;

        // Store the latest tracking update in Redis for quick access
        Map<String, Object> cacheData = Map.of(
                "orderNumber", orderNumber,
                "lastUpdate", message,
                "timestamp", System.currentTimeMillis()
        );

        redisTemplate.opsForValue().set(cacheKey, cacheData, 2, TimeUnit.HOURS);
    }

    private void cacheDriverLocation(String driverId, Double latitude, Double longitude, Map<String, Object> message) {
        String cacheKey = "driver:location:" + driverId;

        Map<String, Object> locationData = Map.of(
                "driverId", driverId,
                "latitude", latitude,
                "longitude", longitude,
                "timestamp", System.currentTimeMillis(),
                "metadata", message
        );

        redisTemplate.opsForValue().set(cacheKey, locationData, 30, TimeUnit.MINUTES);
    }

    private boolean shouldSendNotification(String status) {
        // Define which status changes should trigger notifications
        return java.util.List.of("PICKED_UP", "EN_ROUTE_DELIVERY", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED")
                .contains(status.toUpperCase());
    }

    private Double getDoubleFromMessage(Map<String, Object> message, String key) {
        Object value = message.get(key);
        if (value == null) return null;

        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Could not parse {} as double: {}", key, value);
                return null;
            }
        }

        return null;
    }

    // ============== PUBLIC API METHODS ==============

    public Map<String, Object> getRealtimeTrackingData(String orderNumber) {
        // Check cache first
        String cacheKey = "tracking:" + orderNumber;
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedData = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedData != null) {
            return cachedData;
        }

        // If not in cache, build from database
        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
        if (trackingOpt.isPresent()) {
            DeliveryTracking tracking = trackingOpt.get();
            Map<String, Object> data = Map.of(
                    "orderNumber", tracking.getOrderNumber(),
                    "status", tracking.getCurrentStatus(),
                    "driverId", tracking.getAssignedDriverId() != null ? tracking.getAssignedDriverId() : "",
                    "latitude", tracking.getLastKnownLatitude() != null ? tracking.getLastKnownLatitude() : 0.0,
                    "longitude", tracking.getLastKnownLongitude() != null ? tracking.getLastKnownLongitude() : 0.0,
                    "lastUpdate", tracking.getUpdatedAt().toString(),
                    "estimatedDelivery", tracking.getEstimatedDeliveryTime() != null ? tracking.getEstimatedDeliveryTime().toString() : null
            );

            // Cache for future requests
            cacheTrackingUpdate(orderNumber, data);
            return data;
        }

        return Map.of("error", "Order not found");
    }

    public Map<String, Object> getDriverLocationData(String driverId) {
        String cacheKey = "driver:location:" + driverId;
        @SuppressWarnings("unchecked")
        Map<String, Object> locationData = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);

        if (locationData != null) {
            return locationData;
        }

        return Map.of("error", "Driver location not available");
    }

    public Map<String, Object> getConnectionStats() {
        return sessionManager.getConnectionStats();
    }

    public boolean hasActiveConnections() {
        return sessionManager.hasActiveConnections();
    }
}