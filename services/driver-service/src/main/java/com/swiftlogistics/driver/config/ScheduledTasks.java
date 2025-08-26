// services/driver-service/src/main/java/com/swiftlogistics/driver/config/ScheduledTasks.java
package com.swiftlogistics.driver.config;

import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private DriverRepository driverRepository;

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @Transactional
    public void cleanupStaleLocationData() {
        logger.debug("Starting cleanup of stale location data");

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
            List<Driver> drivers = driverRepository.findDriversWithRecentLocationUpdates(cutoff);

            int updated = 0;
            for (Driver driver : drivers) {
                if (driver.getLastLocationUpdate().isBefore(cutoff)) {
                    // Clear stale location data
                    driver.setCurrentLatitude(null);
                    driver.setCurrentLongitude(null);
                    driver.setLastLocationUpdate(null);
                    driverRepository.save(driver);
                    updated++;
                }
            }

            if (updated > 0) {
                logger.info("Cleaned up stale location data for {} drivers", updated);
            }

        } catch (Exception e) {
            logger.error("Error during location data cleanup: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void updateDriverStatuses() {
        logger.debug("Starting driver status updates");

        try {
            // Mark drivers as offline if they haven't updated location in 30 minutes
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
            List<Driver> activeDrivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);

            int statusUpdated = 0;
            for (Driver driver : activeDrivers) {
                if (driver.getLastLocationUpdate() != null &&
                        driver.getLastLocationUpdate().isBefore(cutoff)) {

                    driver.updateStatus(DriverStatus.OFFLINE);
                    driverRepository.save(driver);
                    statusUpdated++;
                }
            }

            if (statusUpdated > 0) {
                logger.info("Updated status to OFFLINE for {} inactive drivers", statusUpdated);
            }

        } catch (Exception e) {
            logger.error("Error during driver status update: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void generateDailyDriverReports() {
        logger.info("Starting daily driver report generation");

        try {
            // This could generate reports, calculate ratings, etc.
            // For now, just log some statistics

            long totalDrivers = driverRepository.count();
            long activeDrivers = driverRepository.countByStatus(DriverStatus.AVAILABLE);
            long offlineDrivers = driverRepository.countByStatus(DriverStatus.OFFLINE);
            long busyDrivers = driverRepository.countByStatus(DriverStatus.BUSY);

            logger.info("Daily Driver Stats - Total: {}, Active: {}, Busy: {}, Offline: {}",
                    totalDrivers, activeDrivers, busyDrivers, offlineDrivers);

        } catch (Exception e) {
            logger.error("Error during daily report generation: {}", e.getMessage(), e);
        }
    }
}