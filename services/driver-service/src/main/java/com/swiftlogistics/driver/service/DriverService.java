// services/driver-service/src/main/java/com/swiftlogistics/driver/service/DriverService.java
// Updated version with proper imports and method fixes

package com.swiftlogistics.driver.service;

import com.swiftlogistics.driver.dto.*;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.enums.VehicleType;
import com.swiftlogistics.driver.repository.DriverRepository;
import com.swiftlogistics.driver.messaging.DriverMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DriverService {

    private static final Logger logger = LoggerFactory.getLogger(DriverService.class);

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private DriverMessageProducer messageProducer;

    public DriverResponse registerDriver(DriverRegistrationRequest request) {
        logger.info("Registering new driver: {}", request.getEmail());

        // Check if driver already exists
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Driver with email already exists: " + request.getEmail());
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new RuntimeException("Driver with license number already exists: " + request.getLicenseNumber());
        }

        try {
            // Create new driver
            Driver driver = new Driver(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPhoneNumber(),
                    request.getLicenseNumber(),
                    request.getVehicleType()
            );

            driver.setVehicleNumber(request.getVehicleNumber());
            driver.setPassword(passwordEncoder.encode(request.getPassword()));

            // Save driver
            driver = driverRepository.save(driver);
            logger.info("Driver registered successfully: {}", driver.getDriverId());

            // Send registration notification
            messageProducer.sendDriverRegistered(driver);

            return mapToDriverResponse(driver);

        } catch (Exception e) {
            logger.error("Error registering driver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register driver: " + e.getMessage());
        }
    }

    public String authenticateDriver(DriverLoginRequest request) {
        logger.info("Authenticating driver: {}", request.getEmail());

        try {
            Driver driver = driverRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + request.getEmail()));

            if (!driver.isActive()) {
                throw new RuntimeException("Driver account is inactive");
            }

            if (!passwordEncoder.matches(request.getPassword(), driver.getPassword())) {
                throw new RuntimeException("Invalid password");
            }

            // Update login time
            driver.recordLogin();
            driverRepository.save(driver);

            // Generate JWT token
            String token = jwtTokenService.generateToken(driver.getDriverId());
            logger.info("Driver authenticated successfully: {}", driver.getDriverId());

            return token;

        } catch (Exception e) {
            logger.error("Error authenticating driver: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriverProfile(String driverId) {
        logger.debug("Fetching driver profile: {}", driverId);

        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

        return mapToDriverResponse(driver);
    }

    public DriverResponse updateDriverStatus(String driverId, DriverStatus status) {
        logger.info("Updating driver {} status to {}", driverId, status);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            DriverStatus oldStatus = driver.getStatus();
            driver.updateStatus(status);
            driver = driverRepository.save(driver);

            // Send status update notification
            messageProducer.sendDriverStatusUpdate(driver, oldStatus, status);

            logger.info("Driver {} status updated from {} to {}", driverId, oldStatus, status);
            return mapToDriverResponse(driver);

        } catch (Exception e) {
            logger.error("Error updating driver status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update driver status: " + e.getMessage());
        }
    }

    public DriverResponse updateDriverLocation(String driverId, LocationUpdateRequest request) {
        logger.debug("Updating location for driver: {}", driverId);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            driver.updateLocation(request.getLatitude(), request.getLongitude());
            driver = driverRepository.save(driver);

            // Send location update notification
            messageProducer.sendDriverLocationUpdate(driver);

            return mapToDriverResponse(driver);

        } catch (Exception e) {
            logger.error("Error updating driver location: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update location: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "availableDrivers", key = "#vehicleType != null ? #vehicleType.toString() : 'ALL'")
    public List<DriverResponse> getAvailableDrivers(VehicleType vehicleType) {
        logger.debug("Fetching available drivers for vehicle type: {}", vehicleType);

        List<Driver> drivers;
        if (vehicleType != null) {
            drivers = driverRepository.findAvailableDriversByVehicleType(DriverStatus.AVAILABLE, vehicleType);
        } else {
            drivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        }

        return drivers.stream()
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getNearbyDrivers(Double latitude, Double longitude, Double radiusKm) {
        logger.debug("Fetching drivers near location: {}, {} within {} km", latitude, longitude, radiusKm);

        // Get drivers with recent location updates (last 15 minutes)
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        List<Driver> drivers = driverRepository.findDriversWithRecentLocationUpdates(since);

        return drivers.stream()
                .filter(driver -> driver.getStatus() == DriverStatus.AVAILABLE)
                .filter(driver -> {
                    if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) {
                        return false;
                    }
                    double distance = calculateDistance(
                            latitude, longitude,
                            driver.getCurrentLatitude(), driver.getCurrentLongitude()
                    );
                    return distance <= radiusKm;
                })
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getTopPerformingDrivers(int limit) {
        logger.debug("Fetching top {} performing drivers", limit);

        List<Driver> drivers = driverRepository.findTopAvailableDrivers();

        return drivers.stream()
                .limit(limit)
                .map(this::mapToDriverResponse)
                .collect(Collectors.toList());
    }

    public void verifyDriver(String driverId) {
        logger.info("Verifying driver: {}", driverId);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            driver.setVerified(true);
            driverRepository.save(driver);

            // Send verification notification
            messageProducer.sendDriverVerified(driver);

            logger.info("Driver {} verified successfully", driverId);

        } catch (Exception e) {
            logger.error("Error verifying driver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to verify driver: " + e.getMessage());
        }
    }

    public void suspendDriver(String driverId, String reason) {
        logger.info("Suspending driver: {} for reason: {}", driverId, reason);

        try {
            Driver driver = driverRepository.findByDriverId(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found: " + driverId));

            driver.updateStatus(DriverStatus.SUSPENDED);
            driver.setActive(false);
            driverRepository.save(driver);

            // Send suspension notification
            messageProducer.sendDriverSuspended(driver, reason);

            logger.info("Driver {} suspended successfully", driverId);

        } catch (Exception e) {
            logger.error("Error suspending driver: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to suspend driver: " + e.getMessage());
        }
    }

    // Helper methods
    private DriverResponse mapToDriverResponse(Driver driver) {
        DriverResponse response = new DriverResponse();
        response.setDriverId(driver.getDriverId());
        response.setFullName(driver.getFullName());
        response.setEmail(driver.getEmail());
        response.setPhoneNumber(driver.getPhoneNumber());
        response.setLicenseNumber(driver.getLicenseNumber());
        response.setVehicleType(driver.getVehicleType());
        response.setVehicleNumber(driver.getVehicleNumber());
        response.setStatus(driver.getStatus());
        response.setProfilePicture(driver.getProfilePicture());
        response.setCurrentLatitude(driver.getCurrentLatitude());
        response.setCurrentLongitude(driver.getCurrentLongitude());
        response.setLastLocationUpdate(driver.getLastLocationUpdate());
        response.setTotalDeliveries(driver.getTotalDeliveries());
        response.setCompletedDeliveries(driver.getCompletedDeliveries());
        response.setFailedDeliveries(driver.getFailedDeliveries());
        response.setRating(driver.getRating());
        response.setCreatedAt(driver.getCreatedAt());
        response.setLastLoginAt(driver.getLastLoginAt());
        response.setActive(driver.isActive());
        response.setVerified(driver.isVerified());
        return response;
    }

    private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Haversine formula for calculating distance between two points
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}