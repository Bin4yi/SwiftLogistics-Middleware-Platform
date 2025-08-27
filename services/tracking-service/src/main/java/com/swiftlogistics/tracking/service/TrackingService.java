// services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/TrackingService.java
package com.swiftlogistics.tracking.service;

import com.swiftlogistics.tracking.dto.TrackingResponse;
import com.swiftlogistics.tracking.entity.DeliveryTracking;
import com.swiftlogistics.tracking.entity.DriverLocation;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.enums.TrackingEventType;
import com.swiftlogistics.tracking.repository.DeliveryTrackingRepository;
import com.swiftlogistics.tracking.repository.DriverLocationRepository;
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
@Transactional
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);
    private static final String TRACKING_CACHE_KEY = "tracking:";
    private static final int CACHE_EXPIRY_MINUTES = 30;

    @Autowired
    private TrackingEventRepository trackingEventRepository;

    @Autowired
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Autowired
    private DriverLocationRepository driverLocationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    public TrackingResponse getOrderTracking(String orderNumber) {
        logger.debug("Fetching tracking information for order: {}", orderNumber);

        // Try cache first
        String cacheKey = TRACKING_CACHE_KEY + orderNumber;
        TrackingResponse cached = (TrackingResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.debug("Returning cached tracking data for order: {}", orderNumber);
            return cached;
        }

        // Fetch from database
        Optional<DeliveryTracking> deliveryTracking = deliveryTrackingRepository.findByOrderNumber(orderNumber);
        if (deliveryTracking.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderNumber);
        }

        DeliveryTracking tracking = deliveryTracking.get();
        List<TrackingEvent> events = trackingEventRepository.findByOrderNumberOrderByTimestampDesc(orderNumber);

        TrackingResponse response = buildTrackingResponse(tracking, events);

        // Cache the result
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);

        return response;
    }

    public List<TrackingResponse> getClientOrdersTracking(String clientId) {
        logger.debug("Fetching tracking information for client: {}", clientId);

        List<DeliveryTracking> deliveries = deliveryTrackingRepository.findByClientIdOrderByUpdatedAtDesc(clientId);

        return deliveries.stream()
                .map(delivery -> {
                    List<TrackingEvent> events = trackingEventRepository
                            .findByOrderNumberOrderByTimestampDesc(delivery.getOrderNumber());
                    return buildTrackingResponse(delivery, events);
                })
                .collect(Collectors.toList());
    }

    public void createOrderTracking(String orderNumber, String clientId, String status) {
        logger.info("Creating tracking record for order: {}", orderNumber);

        DeliveryTracking tracking = new DeliveryTracking(orderNumber, clientId, status);
        deliveryTrackingRepository.save(tracking);

        // Create initial tracking event
        TrackingEvent event = new TrackingEvent(orderNumber, TrackingEventType.ORDER_CREATED,
                "Order has been created and is being processed");
        trackingEventRepository.save(event);

        // Clear cache
        clearTrackingCache(orderNumber);

        logger.info("Tracking record created for order: {}", orderNumber);
    }

    public void updateOrderStatus(String orderNumber, String newStatus, String driverId, String description) {
        logger.info("Updating order status: {} -> {}", orderNumber, newStatus);

        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
        if (trackingOpt.isEmpty()) {
            logger.warn("Tracking record not found for order: {}, creating new one", orderNumber);
            createOrderTracking(orderNumber, "unknown", newStatus);
            return;
        }

        DeliveryTracking tracking = trackingOpt.get();
        String oldStatus = tracking.getCurrentStatus();
        tracking.setCurrentStatus(newStatus);

        if (driverId != null) {
            tracking.setAssignedDriverId(driverId);
        }

        deliveryTrackingRepository.save(tracking);

        // Create tracking event
        TrackingEventType eventType = mapStatusToEventType(newStatus);
        TrackingEvent event = new TrackingEvent(orderNumber, eventType,
                description != null ? description : "Status updated to " + newStatus);
        event.setDriverId(driverId);
        trackingEventRepository.save(event);

        // Clear cache
        clearTrackingCache(orderNumber);

        logger.info("Order status updated: {} from {} to {}", orderNumber, oldStatus, newStatus);
    }

    public void updateDriverLocation(String driverId, Double latitude, Double longitude,
                                     Double speed, Integer heading, Double accuracy) {
        logger.debug("Updating location for driver: {}", driverId);

        DriverLocation location = new DriverLocation(driverId, latitude, longitude);
        location.setSpeed(speed);
        location.setHeading(heading);
        location.setAccuracy(accuracy);

        driverLocationRepository.save(location);

        // Update delivery tracking records for this driver
        List<DeliveryTracking> activeDeliveries = deliveryTrackingRepository
                .findByAssignedDriverIdOrderByUpdatedAtDesc(driverId);

        for (DeliveryTracking delivery : activeDeliveries) {
            if (isActiveStatus(delivery.getCurrentStatus())) {
                delivery.setLastKnownLatitude(latitude);
                delivery.setLastKnownLongitude(longitude);
                delivery.setLastLocationUpdate(LocalDateTime.now());
                deliveryTrackingRepository.save(delivery);

                // Create location update event
                TrackingEvent event = new TrackingEvent(delivery.getOrderNumber(),
                        TrackingEventType.LOCATION_UPDATE, "Driver location updated");
                event.setDriverId(driverId);
                event.setLatitude(latitude);
                event.setLongitude(longitude);
                trackingEventRepository.save(event);

                // Clear cache for this order
                clearTrackingCache(delivery.getOrderNumber());
            }
        }

        logger.debug("Location updated for driver: {} at ({}, {})", driverId, latitude, longitude);
    }

    public void assignDriverToOrder(String orderNumber, String driverId) {
        logger.info("Assigning driver {} to order {}", driverId, orderNumber);

        Optional<DeliveryTracking> trackingOpt = deliveryTrackingRepository.findByOrderNumber(orderNumber);
        if (trackingOpt.isEmpty()) {
            throw new RuntimeException("Order tracking not found: " + orderNumber);
        }

        DeliveryTracking tracking = trackingOpt.get();
        tracking.setAssignedDriverId(driverId);
        tracking.setCurrentStatus("ASSIGNED_TO_DRIVER");
        deliveryTrackingRepository.save(tracking);

        // Create tracking event
        TrackingEvent event = new TrackingEvent(orderNumber, TrackingEventType.DRIVER_ASSIGNED,
                "Driver has been assigned to this order");
        event.setDriverId(driverId);
        trackingEventRepository.save(event);

        // Clear cache
        clearTrackingCache(orderNumber);

        // Send notification
        notificationService.sendDriverAssignmentNotification(orderNumber, driverId);

        logger.info("Driver {} assigned to order {}", driverId, orderNumber);
    }

    private TrackingResponse buildTrackingResponse(DeliveryTracking tracking, List<TrackingEvent> events) {
        TrackingResponse response = new TrackingResponse();
        response.setOrderNumber(tracking.getOrderNumber());
        response.setCurrentStatus(tracking.getCurrentStatus());
        response.setAssignedDriverId(tracking.getAssignedDriverId());
        response.setEstimatedDeliveryTime(tracking.getEstimatedDeliveryTime());
        response.setCurrentLatitude(tracking.getLastKnownLatitude());
        response.setCurrentLongitude(tracking.getLastKnownLongitude());
        response.setLastLocationUpdate(tracking.getLastLocationUpdate());
        response.setTrackingHistory(events);
        response.setStatusDescription(getStatusDescription(tracking.getCurrentStatus()));

        return response;
    }

    private TrackingEventType mapStatusToEventType(String status) {
        switch (status.toUpperCase()) {
            case "CREATED": return TrackingEventType.ORDER_CREATED;
            case "CONFIRMED": return TrackingEventType.ORDER_CONFIRMED;
            case "ASSIGNED_TO_DRIVER": return TrackingEventType.ORDER_ASSIGNED_TO_DRIVER;
            case "PICKED_UP": return TrackingEventType.ORDER_PICKED_UP;
            case "IN_TRANSIT": return TrackingEventType.ORDER_IN_TRANSIT;
            case "OUT_FOR_DELIVERY": return TrackingEventType.ORDER_OUT_FOR_DELIVERY;
            case "DELIVERED": return TrackingEventType.ORDER_DELIVERED;
            case "FAILED": return TrackingEventType.ORDER_FAILED;
            case "CANCELLED": return TrackingEventType.ORDER_CANCELLED;
            default: return TrackingEventType.STATUS_UPDATE;
        }
    }

    private boolean isActiveStatus(String status) {
        return List.of("ASSIGNED_TO_DRIVER", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY")
                .contains(status.toUpperCase());
    }

    private String getStatusDescription(String status) {
        switch (status.toUpperCase()) {
            case "CREATED": return "Order has been created and is being processed";
            case "CONFIRMED": return "Order has been confirmed and is being prepared";
            case "ASSIGNED_TO_DRIVER": return "Order has been assigned to a driver";
            case "PICKED_UP": return "Order has been picked up by the driver";
            case "IN_TRANSIT": return "Order is in transit to delivery location";
            case "OUT_FOR_DELIVERY": return "Order is out for delivery";
            case "DELIVERED": return "Order has been successfully delivered";
            case "FAILED": return "Delivery attempt failed";
            case "CANCELLED": return "Order has been cancelled";
            default: return "Status: " + status;
        }
    }

    private void clearTrackingCache(String orderNumber) {
        String cacheKey = TRACKING_CACHE_KEY + orderNumber;
        redisTemplate.delete(cacheKey);
    }
}