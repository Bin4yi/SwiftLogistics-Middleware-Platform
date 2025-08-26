// services/driver-service/src/main/java/com/swiftlogistics/driver/service/DeliveryService.java
package com.swiftlogistics.driver.service;

import com.swiftlogistics.driver.dto.*;
import com.swiftlogistics.driver.entity.Delivery;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DeliveryStatus;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.repository.DeliveryRepository;
import com.swiftlogistics.driver.repository.DriverRepository;
import com.swiftlogistics.driver.service.messaging.DriverMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DriverMessageProducer messageProducer;

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDriverDeliveries(String driverId) {
        logger.debug("Fetching deliveries for driver: {}", driverId);

        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

        List<Delivery> deliveries = deliveryRepository.findByDriverIdOrderByRouteSequence(driverId);

        return deliveries.stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getDriverActiveDeliveries(String driverId) {
        logger.debug("Fetching active deliveries for driver: {}", driverId);

        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

        List<Delivery> deliveries = deliveryRepository.findActiveDeliveriesByDriver(driver);

        return deliveries.stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> getTodaysDeliveries(String driverId) {
        logger.debug("Fetching today's deliveries for driver: {}", driverId);

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Delivery> deliveries = deliveryRepository.findByDriverIdAndDateRange(driverId, startOfDay, endOfDay);

        return deliveries.stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    public DeliveryResponse updateDeliveryStatus(String orderNumber, String driverId, DeliveryUpdateRequest request) {
        logger.info("Updating delivery {} status to {} by driver {}", orderNumber, request.getStatus(), driverId);

        try {
            // Verify driver
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            // Find delivery
            Delivery delivery = deliveryRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Delivery not found: " + orderNumber));

            // Verify delivery belongs to driver
            if (!delivery.getDriver().getDriverId().equals(driverId)) {
                throw new RuntimeException("Delivery not assigned to this driver");
            }

            // Validate status transition
            validateStatusTransition(delivery.getStatus(), request.getStatus());

            // Update delivery
            DeliveryStatus oldStatus = delivery.getStatus();
            delivery.updateStatus(request.getStatus());
            delivery.setDeliveryNotes(request.getNotes());
            delivery.setFailureReason(request.getFailureReason());
            delivery.setProofOfDeliveryPhoto(request.getProofOfDeliveryPhoto());
            delivery.setDigitalSignature(request.getDigitalSignature());

            // Update location if provided
            if (request.getLatitude() != null && request.getLongitude() != null) {
                if (request.getStatus() == DeliveryStatus.PICKED_UP) {
                    delivery.setPickupLatitude(request.getLatitude());
                    delivery.setPickupLongitude(request.getLongitude());
                } else if (request.getStatus() == DeliveryStatus.DELIVERED) {
                    delivery.setDeliveryLatitude(request.getLatitude());
                    delivery.setDeliveryLongitude(request.getLongitude());
                }
            }

            delivery = deliveryRepository.save(delivery);

            // Update driver statistics
            updateDriverStatistics(driver, request.getStatus());

            // Update driver status based on delivery status
            updateDriverStatusBasedOnDeliveries(driver);

            // Send status update notification
            messageProducer.sendDeliveryStatusUpdate(delivery, oldStatus, request.getStatus());

            logger.info("Delivery {} status updated from {} to {}", orderNumber, oldStatus, request.getStatus());

            return mapToDeliveryResponse(delivery);

        } catch (Exception e) {
            logger.error("Error updating delivery status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update delivery status: " + e.getMessage());
        }
    }

    public DeliveryResponse acknowledgeDelivery(String orderNumber, String driverId) {
        logger.info("Driver {} acknowledging delivery {}", driverId, orderNumber);

        DeliveryUpdateRequest request = new DeliveryUpdateRequest();
        request.setStatus(DeliveryStatus.ACKNOWLEDGED);
        request.setNotes("Delivery acknowledged by driver");

        return updateDeliveryStatus(orderNumber, driverId, request);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDeliveryDetails(String orderNumber, String driverId) {
        logger.debug("Fetching delivery details: {} for driver: {}", orderNumber, driverId);

        Delivery delivery = deliveryRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Delivery not found: " + orderNumber));

        // Verify delivery belongs to driver
        if (!delivery.getDriver().getDriverId().equals(driverId)) {
            throw new RuntimeException("Delivery not assigned to this driver");
        }

        return mapToDeliveryResponse(delivery);
    }

    // Message listener for new delivery assignments
    @Transactional
    public void assignDeliveryToDriver(String orderNumber, String driverId) {
        logger.info("Assigning delivery {} to driver {}", orderNumber, driverId);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            // Check if delivery already exists (from integration service)
            Delivery delivery = deliveryRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new RuntimeException("Delivery record not found: " + orderNumber));

            // Assign to driver
            delivery.setDriver(driver);
            delivery.setAssignedAt(LocalDateTime.now());
            delivery.updateStatus(DeliveryStatus.ASSIGNED);

            deliveryRepository.save(delivery);

            // Update driver status if they were available
            if (driver.getStatus() == DriverStatus.AVAILABLE) {
                driver.updateStatus(DriverStatus.BUSY);
                driverRepository.save(driver);
            }

            // Send assignment notification
            messageProducer.sendDeliveryAssigned(delivery);

            logger.info("Delivery {} assigned to driver {} successfully", orderNumber, driverId);

        } catch (Exception e) {
            logger.error("Error assigning delivery to driver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign delivery: " + e.getMessage());
        }
    }

    // Helper methods
    private void validateStatusTransition(DeliveryStatus currentStatus, DeliveryStatus newStatus) {
        // Define valid status transitions
        switch (currentStatus) {
            case ASSIGNED:
                if (newStatus != DeliveryStatus.ACKNOWLEDGED && newStatus != DeliveryStatus.CANCELLED) {
                    throw new RuntimeException("Invalid status transition from ASSIGNED to " + newStatus);
                }
                break;
            case ACKNOWLEDGED:
                if (newStatus != DeliveryStatus.EN_ROUTE_PICKUP && newStatus != DeliveryStatus.CANCELLED) {
                    throw new RuntimeException("Invalid status transition from ACKNOWLEDGED to " + newStatus);
                }
                break;
            case EN_ROUTE_PICKUP:
                if (newStatus != DeliveryStatus.AT_PICKUP && newStatus != DeliveryStatus.CANCELLED) {
                    throw new RuntimeException("Invalid status transition from EN_ROUTE_PICKUP to " + newStatus);
                }
                break;
            case AT_PICKUP:
                if (newStatus != DeliveryStatus.PICKED_UP && newStatus != DeliveryStatus.FAILED) {
                    throw new RuntimeException("Invalid status transition from AT_PICKUP to " + newStatus);
                }
                break;
            case PICKED_UP:
                if (newStatus != DeliveryStatus.EN_ROUTE_DELIVERY) {
                    throw new RuntimeException("Invalid status transition from PICKED_UP to " + newStatus);
                }
                break;
            case EN_ROUTE_DELIVERY:
                if (newStatus != DeliveryStatus.AT_DELIVERY) {
                    throw new RuntimeException("Invalid status transition from EN_ROUTE_DELIVERY to " + newStatus);
                }
                break;
            case AT_DELIVERY:
                if (newStatus != DeliveryStatus.DELIVERED && newStatus != DeliveryStatus.FAILED) {
                    throw new RuntimeException("Invalid status transition from AT_DELIVERY to " + newStatus);
                }
                break;
            default:
                if (newStatus != DeliveryStatus.CANCELLED && newStatus != DeliveryStatus.RETURNED) {
                    throw new RuntimeException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
        }
    }

    private void updateDriverStatistics(Driver driver, DeliveryStatus status) {
        switch (status) {
            case DELIVERED:
                driver.setCompletedDeliveries(driver.getCompletedDeliveries() + 1);
                driver.setTotalDeliveries(driver.getTotalDeliveries() + 1);
                break;
            case FAILED:
                driver.setFailedDeliveries(driver.getFailedDeliveries() + 1);
                driver.setTotalDeliveries(driver.getTotalDeliveries() + 1);
                break;
        }

        // Recalculate rating based on completion rate
        if (driver.getTotalDeliveries() > 0) {
            double completionRate = (double) driver.getCompletedDeliveries() / driver.getTotalDeliveries();
            driver.setRating(completionRate * 5.0); // Scale to 5-star rating
        }

        driverRepository.save(driver);
    }

    private void updateDriverStatusBasedOnDeliveries(Driver driver) {
        // Check if driver has any active deliveries
        List<Delivery> activeDeliveries = deliveryRepository.findActiveDeliveriesByDriver(driver);

        if (activeDeliveries.isEmpty() && driver.getStatus() == DriverStatus.BUSY) {
            // No active deliveries, mark as available
            driver.updateStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);
            logger.info("Driver {} status updated to AVAILABLE (no active deliveries)", driver.getDriverId());
        }
    }

    private DeliveryResponse mapToDeliveryResponse(Delivery delivery) {
        DeliveryResponse response = new DeliveryResponse();
        response.setId(delivery.getId());
        response.setOrderNumber(delivery.getOrderNumber());
        response.setClientId(delivery.getClientId());
        response.setDriverId(delivery.getDriver() != null ? delivery.getDriver().getDriverId() : null);
        response.setPickupAddress(delivery.getPickupAddress());
        response.setDeliveryAddress(delivery.getDeliveryAddress());
        response.setPackageDescription(delivery.getPackageDescription());
        response.setPriority(delivery.getPriority());
        response.setStatus(delivery.getStatus());
        response.setScheduledDate(delivery.getScheduledDate());
        response.setAssignedAt(delivery.getAssignedAt());
        response.setPickedUpAt(delivery.getPickedUpAt());
        response.setDeliveredAt(delivery.getDeliveredAt());
        response.setRecipientName(delivery.getRecipientName());
        response.setRecipientPhone(delivery.getRecipientPhone());
        response.setSpecialInstructions(delivery.getSpecialInstructions());
        response.setProofOfDeliveryPhoto(delivery.getProofOfDeliveryPhoto());
        response.setDigitalSignature(delivery.getDigitalSignature());
        response.setDeliveryNotes(delivery.getDeliveryNotes());
        response.setFailureReason(delivery.getFailureReason());
        response.setPickupLatitude(delivery.getPickupLatitude());
        response.setPickupLongitude(delivery.getPickupLongitude());
        response.setDeliveryLatitude(delivery.getDeliveryLatitude());
        response.setDeliveryLongitude(delivery.getDeliveryLongitude());
        response.setRouteSequence(delivery.getRouteSequence());
        response.setRouteId(delivery.getRouteId());
        return response;
    }

}