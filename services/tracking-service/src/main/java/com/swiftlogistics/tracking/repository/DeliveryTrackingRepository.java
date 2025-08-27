// services/tracking-service/src/main/java/com/swiftlogistics/tracking/repository/DeliveryTrackingRepository.java
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

    Optional<DeliveryTracking> findByOrderNumber(String orderNumber);

    List<DeliveryTracking> findByClientIdOrderByUpdatedAtDesc(String clientId);

    List<DeliveryTracking> findByAssignedDriverIdOrderByUpdatedAtDesc(String assignedDriverId);

    List<DeliveryTracking> findByCurrentStatusOrderByUpdatedAtDesc(String currentStatus);

    @Query("SELECT dt FROM DeliveryTracking dt WHERE dt.currentStatus IN :statuses ORDER BY dt.updatedAt DESC")
    List<DeliveryTracking> findByCurrentStatusInOrderByUpdatedAtDesc(@Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(dt) FROM DeliveryTracking dt WHERE dt.currentStatus = :status")
    long countByCurrentStatus(@Param("status") String status);
}