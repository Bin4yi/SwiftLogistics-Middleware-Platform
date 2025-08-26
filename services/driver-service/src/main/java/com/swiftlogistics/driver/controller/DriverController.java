// services/driver-service/src/main/java/com/swiftlogistics/driver/controller/DriverController.java
package com.swiftlogistics.driver.controller;

import com.swiftlogistics.driver.dto.*;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.enums.VehicleType;
import com.swiftlogistics.driver.service.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    private static final Logger logger = LoggerFactory.getLogger(DriverController.class);

    @Autowired
    private DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DriverResponse>> registerDriver(
            @Valid @RequestBody DriverRegistrationRequest request) {

        logger.info("Driver registration request for: {}", request.getEmail());

        try {
            DriverResponse driver = driverService.registerDriver(request);
            return ResponseEntity.ok(ApiResponse.success("Driver registered successfully. Pending verification.", driver));

        } catch (Exception e) {
            logger.error("Error registering driver: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to register driver: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginDriver(
            @Valid @RequestBody DriverLoginRequest request) {

        logger.info("Driver login request for: {}", request.getEmail());

        try {
            String token = driverService.authenticateDriver(request);

            Map<String, Object> response = Map.of(
                    "token", token,
                    "tokenType", "Bearer",
                    "message", "Login successful"
            );

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (Exception e) {
            logger.error("Error authenticating driver: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverProfile(HttpServletRequest request) {
        String driverId = getDriverIdFromRequest(request);
        logger.debug("Fetching profile for driver: {}", driverId);

        try {
            DriverResponse driver = driverService.getDriverProfile(driverId);
            return ResponseEntity.ok(ApiResponse.success(driver));

        } catch (Exception e) {
            logger.error("Error fetching driver profile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/status")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriverStatus(
            @RequestParam DriverStatus status,
            HttpServletRequest request) {

        String driverId = getDriverIdFromRequest(request);
        logger.info("Updating status for driver {} to {}", driverId, status);

        try {
            DriverResponse driver = driverService.updateDriverStatus(driverId, status);
            return ResponseEntity.ok(ApiResponse.success("Status updated successfully", driver));

        } catch (Exception e) {
            logger.error("Error updating driver status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @PutMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateLocation(
            @Valid @RequestBody LocationUpdateRequest locationRequest,
            HttpServletRequest request) {

        String driverId = getDriverIdFromRequest(request);
        logger.debug("Updating location for driver: {}", driverId);

        try {
            DriverResponse driver = driverService.updateDriverLocation(driverId, locationRequest);
            return ResponseEntity.ok(ApiResponse.success("Location updated successfully", driver));

        } catch (Exception e) {
            logger.error("Error updating driver location: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update location: " + e.getMessage()));
        }
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAvailableDrivers(
            @RequestParam(required = false) VehicleType vehicleType) {

        logger.debug("Fetching available drivers for vehicle type: {}", vehicleType);

        try {
            List<DriverResponse> drivers = driverService.getAvailableDrivers(vehicleType);
            return ResponseEntity.ok(ApiResponse.success(drivers));

        } catch (Exception e) {
            logger.error("Error fetching available drivers: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch drivers: " + e.getMessage()));
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getNearbyDrivers(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        logger.debug("Fetching drivers near {}, {} within {} km", latitude, longitude, radiusKm);

        try {
            List<DriverResponse> drivers = driverService.getNearbyDrivers(latitude, longitude, radiusKm);
            return ResponseEntity.ok(ApiResponse.success(drivers));

        } catch (Exception e) {
            logger.error("Error fetching nearby drivers: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch nearby drivers: " + e.getMessage()));
        }
    }

    @GetMapping("/top-performers")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getTopPerformers(
            @RequestParam(defaultValue = "10") int limit) {

        logger.debug("Fetching top {} performing drivers", limit);

        try {
            List<DriverResponse> drivers = driverService.getTopPerformingDrivers(limit);
            return ResponseEntity.ok(ApiResponse.success(drivers));

        } catch (Exception e) {
            logger.error("Error fetching top performers: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch top performers: " + e.getMessage()));
        }
    }

    // Admin endpoints
    @PutMapping("/{driverId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> verifyDriver(@PathVariable String driverId) {
        logger.info("Admin verification request for driver: {}", driverId);

        try {
            driverService.verifyDriver(driverId);
            return ResponseEntity.ok(ApiResponse.success("Driver verified successfully", null));

        } catch (Exception e) {
            logger.error("Error verifying driver: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to verify driver: " + e.getMessage()));
        }
    }

    @PutMapping("/{driverId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> suspendDriver(
            @PathVariable String driverId,
            @RequestParam String reason) {

        logger.info("Admin suspension request for driver: {} - {}", driverId, reason);

        try {
            driverService.suspendDriver(driverId, reason);
            return ResponseEntity.ok(ApiResponse.success("Driver suspended successfully", null));

        } catch (Exception e) {
            logger.error("Error suspending driver: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to suspend driver: " + e.getMessage()));
        }
    }

    private String getDriverIdFromRequest(HttpServletRequest request) {
        // Extract driver ID from JWT token in Authorization header
        return (String) request.getAttribute("driverId");
    }
}