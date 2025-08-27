// services/tracking-service/src/main/java/com/swiftlogistics/tracking/service/LocationTrackingService.java
package com.swiftlogistics.tracking.service;

import com.swiftlogistics.tracking.dto.LocationUpdateRequest;
import com.swiftlogistics.tracking.entity.DriverLocation;
import com.swiftlogistics.tracking.repository.DriverLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class LocationTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(LocationTrackingService.class);
    private static final String LOCATION_CACHE_KEY = "driver:location:";
    private static final int CACHE_EXPIRY_MINUTES = 5;

    @Autowired
    private DriverLocationRepository driverLocationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private NotificationService notificationService;

    public void updateDriverLocation(LocationUpdateRequest request) {
        logger.debug("Processing location update for driver: {}", request.getDriverId());

        // Save to database
        trackingService.updateDriverLocation(
                request.getDriverId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getSpeed(),
                request.getHeading(),
                request.getAccuracy()
        );

        // Cache current location in Redis for fast access
        String cacheKey = LOCATION_CACHE_KEY + request.getDriverId();
        redisTemplate.opsForValue().set(cacheKey, request, CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES);

        logger.debug("Location updated and cached for driver: {}", request.getDriverId());
    }

    public Optional<LocationUpdateRequest> getCurrentDriverLocation(String driverId) {
        logger.debug("Fetching current location for driver: {}", driverId);

        // Try cache first
        String cacheKey = LOCATION_CACHE_KEY + driverId;
        LocationUpdateRequest cached = (LocationUpdateRequest) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Fallback to database
        Optional<DriverLocation> latest = driverLocationRepository.findLatestByDriverId(driverId);
        if (latest.isPresent()) {
            DriverLocation location = latest.get();
            LocationUpdateRequest request = new LocationUpdateRequest(
                    location.getDriverId(),
                    location.getLatitude(),
                    location.getLongitude()
            );
            request.setSpeed(location.getSpeed());
            request.setHeading(location.getHeading());
            request.setAccuracy(location.getAccuracy());
            return Optional.of(request);
        }

        return Optional.empty();
    }

    public List<DriverLocation> getDriverLocationHistory(String driverId, int hours) {
        logger.debug("Fetching location history for driver: {} for last {} hours", driverId, hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return driverLocationRepository.findByDriverIdAndTimestampBetween(
                driverId, since, LocalDateTime.now()
        );
    }

    public List<String> getActiveDrivers() {
        logger.debug("Fetching list of active drivers");

        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        return driverLocationRepository.findActiveDriversSince(since);
    }
}