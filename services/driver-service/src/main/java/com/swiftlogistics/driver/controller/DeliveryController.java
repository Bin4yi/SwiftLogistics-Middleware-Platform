// services/driver-service/src/main/java/com/swiftlogistics/driver/controller/DeliveryController.java
package com.swiftlogistics.driver.controller;

import com.swiftlogistics.driver.dto.ApiResponse;
import com.swiftlogistics.driver.dto.DeliveryResponse;
import com.swiftlogistics.driver.dto.DeliveryUpdateRequest;
import com.swiftlogistics.driver.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/drivers/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getDriverDeliveries(HttpServletRequest request) {
        String driverId = getDriverIdFromRequest(request);
        logger.debug("Fetching deliveries for driver: {}", driverId);

        try {
            List<DeliveryResponse> deliveries = deliveryService.getDriverDeliveries(driverId);
            return ResponseEntity.ok(ApiResponse.success(deliveries));

        } catch (Exception e) {
            logger.error("Error fetching driver deliveries: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch deliveries: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getActiveDeliveries(HttpServletRequest request) {
        String driverId = getDriverIdFromRequest(request);
        logger.debug("Fetching active deliveries for driver: {}", driverId);

        try {
            List<DeliveryResponse> deliveries = deliveryService.getDriverActiveDeliveries(driverId);
            return ResponseEntity.ok(ApiResponse.success(deliveries));

        } catch (Exception e) {
            logger.error("Error fetching active deliveries: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch active deliveries: " + e.getMessage()));
        }
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<DeliveryResponse>>> getTodaysDeliveries(HttpServletRequest request) {
        String driverId = getDriverIdFromRequest(request);
        logger.debug("Fetching today's deliveries for driver: {}", driverId);

        try {
            List<DeliveryResponse> deliveries = deliveryService.getTodaysDeliveries(driverId);
            return ResponseEntity.ok(ApiResponse.success(deliveries));

        } catch (Exception e) {
            logger.error("Error fetching today's deliveries: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch today's deliveries: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryDetails(
            @PathVariable String orderNumber,
            HttpServletRequest request) {

        String driverId = getDriverIdFromRequest(request);
        logger.debug("Fetching delivery details: {} for driver: {}", orderNumber, driverId);

        try {
            DeliveryResponse delivery = deliveryService.getDeliveryDetails(orderNumber, driverId);
            return ResponseEntity.ok(ApiResponse.success(delivery));

        } catch (Exception e) {
            logger.error("Error fetching delivery details: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{orderNumber}/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody DeliveryUpdateRequest updateRequest,
            HttpServletRequest request) {

        String driverId = getDriverIdFromRequest(request);
        logger.info("Updating delivery {} status to {} by driver {}",
                orderNumber, updateRequest.getStatus(), driverId);

        try {
            DeliveryResponse delivery = deliveryService.updateDeliveryStatus(orderNumber, driverId, updateRequest);
            return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", delivery));

        } catch (Exception e) {
            logger.error("Error updating delivery status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update delivery status: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderNumber}/acknowledge")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DeliveryResponse>> acknowledgeDelivery(
            @PathVariable String orderNumber,
            HttpServletRequest request) {

        String driverId = getDriverIdFromRequest(request);
        logger.info("Driver {} acknowledging delivery {}", driverId, orderNumber);

        try {
            DeliveryResponse delivery = deliveryService.acknowledgeDelivery(orderNumber, driverId);
            return ResponseEntity.ok(ApiResponse.success("Delivery acknowledged successfully", delivery));

        } catch (Exception e) {
            logger.error("Error acknowledging delivery: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to acknowledge delivery: " + e.getMessage()));
        }
    }

    private String getDriverIdFromRequest(HttpServletRequest request) {
        return (String) request.getAttribute("driverId");
    }
}
