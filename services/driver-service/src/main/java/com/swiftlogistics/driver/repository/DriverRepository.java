// services/driver-service/src/main/java/com/swiftlogistics/driver/repository/DriverRepository.java
package com.swiftlogistics.driver.repository;

import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DriverStatus;
import com.swiftlogistics.driver.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByDriverId(String driverId);

    Optional<Driver> findByEmail(String email);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    List<Driver> findByStatus(DriverStatus status);

    List<Driver> findByVehicleType(VehicleType vehicleType);

    List<Driver> findByActiveTrue();

    List<Driver> findByVerifiedTrue();

    @Query("SELECT d FROM Driver d WHERE d.status = :status AND d.vehicleType = :vehicleType AND d.active = true AND d.verified = true")
    List<Driver> findAvailableDriversByVehicleType(@Param("status") DriverStatus status,
                                                   @Param("vehicleType") VehicleType vehicleType);

    @Query("SELECT d FROM Driver d WHERE d.currentLatitude IS NOT NULL AND d.currentLongitude IS NOT NULL " +
            "AND d.lastLocationUpdate > :since")
    List<Driver> findDriversWithRecentLocationUpdates(@Param("since") LocalDateTime since);

    @Query("SELECT d FROM Driver d WHERE d.status = 'AVAILABLE' AND d.active = true AND d.verified = true " +
            "ORDER BY d.rating DESC, d.completedDeliveries DESC")
    List<Driver> findTopAvailableDrivers();

    @Query("SELECT COUNT(d) FROM Driver d WHERE d.status = :status")
    long countByStatus(@Param("status") DriverStatus status);

    @Query("SELECT d FROM Driver d WHERE d.fullName LIKE %:name%")
    List<Driver> findByFullNameContaining(@Param("name") String name);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);
}