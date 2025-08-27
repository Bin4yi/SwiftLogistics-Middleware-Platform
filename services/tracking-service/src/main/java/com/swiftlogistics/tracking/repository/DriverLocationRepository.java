// services/tracking-service/src/main/java/com/swiftlogistics/tracking/repository/DriverLocationRepository.java
package com.swiftlogistics.tracking.repository;

import com.swiftlogistics.tracking.entity.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {

    @Query("SELECT dl FROM DriverLocation dl WHERE dl.driverId = :driverId ORDER BY dl.timestamp DESC")
    List<DriverLocation> findByDriverIdOrderByTimestampDesc(@Param("driverId") String driverId);

    @Query("SELECT dl FROM DriverLocation dl WHERE dl.driverId = :driverId " +
            "ORDER BY dl.timestamp DESC LIMIT 1")
    Optional<DriverLocation> findLatestByDriverId(@Param("driverId") String driverId);

    @Query("SELECT dl FROM DriverLocation dl WHERE dl.driverId = :driverId " +
            "AND dl.timestamp BETWEEN :startTime AND :endTime ORDER BY dl.timestamp DESC")
    List<DriverLocation> findByDriverIdAndTimestampBetween(
            @Param("driverId") String driverId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT DISTINCT dl.driverId FROM DriverLocation dl WHERE dl.timestamp >= :since")
    List<String> findActiveDriversSince(@Param("since") LocalDateTime since);
}