// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/repository/DeliveryTrackingRepository.java
// ADD THESE METHODS TO YOUR EXISTING REPOSITORY

package com.swiftlogistics.tracking.repository;

import com.swiftlogistics.tracking.entity.DeliveryTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {

    // EXISTING METHOD
    Optional<DeliveryTracking> findByOrderNumber(String orderNumber);

    // ADD THESE NEW METHODS FOR REAL-TIME TRACKING
    List<DeliveryTracking> findByAssignedDriverId(String driverId);

    List<DeliveryTracking> findByClientId(String clientId);

    List<DeliveryTracking> findByCurrentStatus(String status);

    List<DeliveryTracking> findByAssignedDriverIdAndCurrentStatusIn(String driverId, List<String> statuses);

    @Query("SELECT dt FROM DeliveryTracking dt WHERE dt.currentStatus IN :statuses ORDER BY dt.updatedAt DESC")
    List<DeliveryTracking> findActiveDeliveries(@Param("statuses") List<String> statuses);

    @Query("SELECT dt FROM DeliveryTracking dt WHERE dt.clientId = :clientId AND dt.currentStatus IN :statuses")
    List<DeliveryTracking> findClientActiveDeliveries(@Param("clientId") String clientId, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(dt) FROM DeliveryTracking dt WHERE dt.currentStatus = :status")
    long countByStatus(@Param("status") String status);
}