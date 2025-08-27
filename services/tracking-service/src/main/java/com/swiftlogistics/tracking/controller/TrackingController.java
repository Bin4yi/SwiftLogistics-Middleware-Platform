// services/tracking-service/src/main/java/com/swiftlogistics/tracking/controller/TrackingController.java
package com.swiftlogistics.tracking.controller;

import com.swiftlogistics.tracking.dto.LocationUpdateRequest;
import com.swiftlogistics.tracking.dto.TrackingResponse;
import com.swiftlogistics.tracking.entity.DriverLocation;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.service.LocationTrackingService;
import com.swiftlogistics.tracking.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
public class TrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private LocationTrackingService locationTrackingService;

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<TrackingResponse> getOrderTracking(@PathVariable String orderNumber) {
        try {
            logger.info("Fetching tracking info for order: {}", orderNumber);
            TrackingResponse tracking = trackingService.getOrderTracking(orderNumber);
            return ResponseEntity.ok(tracking);
        } catch (RuntimeException e) {
            logger.error("Error fetching order tracking: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Unexpected error fetching order tracking: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/clients/{clientId}/orders")
    public ResponseEntity<List<TrackingResponse>> getClientOrdersTracking(@PathVariable String clientId) {
        try {
            logger.info("Fetching tracking info for client: {}", clientId);
            List<TrackingResponse> trackingList = trackingService.getClientOrdersTracking(clientId);
            return ResponseEntity.ok(trackingList);
        } catch (Exception e) {
            logger.error("Error fetching client orders tracking: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/location")
    public ResponseEntity<String> updateDriverLocation(@Valid @RequestBody LocationUpdateRequest request) {
        try {
            logger.info("Updating location for driver: {}", request.getDriverId());
            locationTrackingService.updateDriverLocation(request);
            return ResponseEntity.ok("Location updated successfully");
        } catch (Exception e) {
            logger.error("Error updating driver location: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update location");
        }
    }

    @GetMapping("/drivers/{driverId}/location")
    public ResponseEntity<LocationUpdateRequest> getDriverCurrentLocation(@PathVariable String driverId) {
        try {
            logger.debug("Fetching current location for driver: {}", driverId);
            Optional<LocationUpdateRequest> location = locationTrackingService.getCurrentDriverLocation(driverId);

            if (location.isPresent()) {
                return ResponseEntity.ok(location.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching driver location: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/drivers/{driverId}/location/history")
    public ResponseEntity<List<DriverLocation>> getDriverLocationHistory(
            @PathVariable String driverId,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            logger.debug("Fetching location history for driver: {} for {} hours", driverId, hours);
            List<DriverLocation> history = locationTrackingService.getDriverLocationHistory(driverId, hours);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error fetching driver location history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/drivers/active")
    public ResponseEntity<List<String>> getActiveDrivers() {
        try {
            logger.debug("Fetching active drivers");
            List<String> activeDrivers = locationTrackingService.getActiveDrivers();
            return ResponseEntity.ok(activeDrivers);
        } catch (Exception e) {
            logger.error("Error fetching active drivers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/orders/{orderNumber}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam String status,
            @RequestParam(required = false) String driverId,
            @RequestParam(required = false) String description) {
        try {
            logger.info("Manually updating order status: {} -> {}", orderNumber, status);
            trackingService.updateOrderStatus(orderNumber, status, driverId, description);
            return ResponseEntity.ok("Order status updated successfully");
        } catch (RuntimeException e) {
            logger.error("Error updating order status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update order status");
        }
    }

    @PostMapping("/orders/{orderNumber}/assign-driver/{driverId}")
    public ResponseEntity<String> assignDriverToOrder(
            @PathVariable String orderNumber,
            @PathVariable String driverId) {
        try {
            logger.info("Assigning driver {} to order {}", driverId, orderNumber);
            trackingService.assignDriverToOrder(orderNumber, driverId);
            return ResponseEntity.ok("Driver assigned successfully");
        } catch (RuntimeException e) {
            logger.error("Error assigning driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error assigning driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to assign driver");
        }
    }
}