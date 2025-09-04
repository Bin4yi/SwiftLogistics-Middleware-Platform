// services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/TrackingService.java
package com.swiftlogistics.tracking.service;

import com.swiftlogistics.tracking.dto.TrackingResponse;
import com.swiftlogistics.tracking.dto.TrackingEventDto;
import com.swiftlogistics.tracking.entity.DeliveryTracking;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.enums.TrackingEventType;
import com.swiftlogistics.tracking.repository.DeliveryTrackingRepository;
import com.swiftlogistics.tracking.repository.TrackingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    private static final String TRACKING_CACHE_PREFIX = "tracking:";
    private static final int CACHE_TTL_MINUTES = 15;

    /**
     * Get tracking information for an order
     */
    public TrackingResponse getOrderTracking(String orderNumber) {
        logger.info("Getting tracking information for order: {}", orderNumber);

        try {
            // Check cache first
            String cacheKey = TRACKING_CACHE_PREFIX + orderNumber;
            TrackingResponse cached = (TrackingResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.debug("Returning cached tracking data for order: {}", orderNumber);
                return cached;
            }

            // Get tracking from database
            Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
            if (trackingOpt.isEmpty()) {
                throw new RuntimeException("Order not found: " + orderNumber);
            }

            DeliveryTracking tracking = trackingOpt.get();

            // Get tracking events
            List<TrackingEvent> events = trackingEventRepository
                    .findByOrderNumberOrderByTimestampDesc(orderNumber);

            List<TrackingEventDto> eventDtos = events.stream()
                    .map(TrackingEventDto::fromEntity)
                    .collect(Collectors.toList());

            // Build response
            TrackingResponse response = new TrackingResponse();
            response.setOrderNumber(tracking.getOrderNumber());
            response.setCurrentStatus(tracking.getCurrentStatus());
            response.setClientId(tracking.getClientId());
            response.setAssignedDriverId(tracking.getAssignedDriverId());
            response.setLastKnownLatitude(tracking.getLastKnownLatitude());
            response.setLastKnownLongitude(tracking.getLastKnownLongitude());
            response.setLastLocationUpdate(tracking.getLastLocationUpdate());
            response.setEstimatedDeliveryTime(tracking.getEstimatedDeliveryTime());
            response.setCreatedAt(tracking.getCreatedAt());
            response.setUpdatedAt(tracking.getUpdatedAt());
            response.setTrackingHistory(eventDtos);

            // Cache the response
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            logger.debug("Successfully retrieved tracking data for order: {}", orderNumber);
            return response;

        } catch (Exception e) {
            logger.error("Error getting tracking for order {}: {}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to get tracking information", e);
        }
    }

    /**
     * Create initial tracking record for a new order
     */
    @Transactional
    public void createOrderTracking(String orderNumber, String clientId, String status) {
        logger.info("Creating tracking record for order: {} with status: {}", orderNumber, status);

        try {
            // Check if tracking already exists
            Optional<DeliveryTracking> existingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
            if (existingOpt.isPresent()) {
                logger.warn("Tracking record already exists for order: {}", orderNumber);
                return;
            }

            // Create delivery tracking record
            DeliveryTracking tracking = new DeliveryTracking(orderNumber, clientId, status);
            tracking.setCreatedAt(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());
            deliveryTrackingRepository.save(tracking);

            // Create initial tracking event
            TrackingEvent event = new TrackingEvent(orderNumber, TrackingEventType.ORDER_CREATED, "Order tracking created");
            event.setTimestamp(LocalDateTime.now());
            trackingEventRepository.save(event);

            // Clear cache
            String cacheKey = TRACKING_CACHE_PREFIX + orderNumber;
            redisTemplate.delete(cacheKey);

            logger.info("Successfully created tracking record for order: {}", orderNumber);

        } catch (Exception e) {
            logger.error("Error creating tracking for order {}: {}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to create tracking record", e);
        }
    }

    /**
     * Update order status and create tracking event
     */
    @Transactional
    public void updateOrderStatus(String orderNumber, String newStatus, String driverId, String description) {
        logger.info("Updating order status for {}: {} (driver: {})", orderNumber, newStatus, driverId);

        try {
            // Get existing tracking record
            Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
            if (trackingOpt.isEmpty()) {
                throw new RuntimeException("Tracking record not found for order: " + orderNumber);
            }

            DeliveryTracking tracking = trackingOpt.get();
            String oldStatus = tracking.getCurrentStatus();

            // Update tracking record
            tracking.setCurrentStatus(newStatus);
            tracking.setUpdatedAt(LocalDateTime.now());
            if (driverId != null) {
                tracking.setAssignedDriverId(driverId);
            }

            deliveryTrackingRepository.save(tracking);

            // Create tracking event
            TrackingEventType eventType = mapStatusToEventType(newStatus);
            TrackingEvent event = new TrackingEvent(orderNumber, eventType, description != null ? description : newStatus);
            event.setDriverId(driverId);
            event.setTimestamp(LocalDateTime.now());
            trackingEventRepository.save(event);

            // Clear cache
            String cacheKey = TRACKING_CACHE_PREFIX + orderNumber;
            redisTemplate.delete(cacheKey);

            // Send notification if significant status change
            if (isSignificantStatusChange(oldStatus, newStatus)) {
                notificationService.sendStatusUpdateNotification(orderNumber, newStatus, tracking.getClientId(), driverId);
            }

            logger.info("Successfully updated order status: {} -> {}", orderNumber, newStatus);

        } catch (Exception e) {
            logger.error("Error updating order status for {}: {}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to update order status", e);
        }
    }

    /**
     * Update driver location for an order
     */
    @Transactional
    public void updateDriverLocation(String orderNumber, String driverId, Double latitude, Double longitude) {
        logger.debug("Updating driver location for order: {} (driver: {})", orderNumber, driverId);

        try {
            // Get tracking record
            Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
            if (trackingOpt.isEmpty()) {
                logger.warn("Tracking record not found for order: {}", orderNumber);
                return;
            }

            DeliveryTracking tracking = trackingOpt.get();

            // Update location
            tracking.setLastKnownLatitude(latitude);
            tracking.setLastKnownLongitude(longitude);
            tracking.setLastLocationUpdate(LocalDateTime.now());
            tracking.setUpdatedAt(LocalDateTime.now());

            deliveryTrackingRepository.save(tracking);

            // Create location update event
            TrackingEvent event = new TrackingEvent(orderNumber, TrackingEventType.LOCATION_UPDATE, "Driver location updated");
            event.setDriverId(driverId);
            event.setLatitude(latitude);
            event.setLongitude(longitude);
            event.setTimestamp(LocalDateTime.now());
            trackingEventRepository.save(event);

            // Clear cache
            String cacheKey = TRACKING_CACHE_PREFIX + orderNumber;
            redisTemplate.delete(cacheKey);

        } catch (Exception e) {
            logger.error("Error updating location for order {}: {}", orderNumber, e.getMessage(), e);
        }
    }

    /**
     * Get tracking events for an order
     */
    public List<TrackingEventDto> getOrderEvents(String orderNumber) {
        logger.debug("Getting tracking events for order: {}", orderNumber);

        List<TrackingEvent> events = trackingEventRepository.findByOrderNumberOrderByTimestampDesc(orderNumber);
        return events.stream()
                .map(TrackingEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    // Helper methods

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

    private boolean isSignificantStatusChange(String oldStatus, String newStatus) {
        // Define which status changes should trigger notifications
        if (oldStatus == null) return true;

        return !oldStatus.equals(newStatus) && (
                "CONFIRMED".equals(newStatus) ||
                        "ASSIGNED".equals(newStatus) ||
                        "PICKED_UP".equals(newStatus) ||
                        "OUT_FOR_DELIVERY".equals(newStatus) ||
                        "DELIVERED".equals(newStatus) ||
                        "FAILED".equals(newStatus) ||
                        "CANCELLED".equals(newStatus)
        );
    }
}