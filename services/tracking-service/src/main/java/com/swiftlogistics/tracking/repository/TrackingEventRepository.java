// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/repository/TrackingEventRepository.java
// CREATE THIS FILE IF IT DOESN'T EXIST OR UPDATE YOUR EXISTING ONE

package com.swiftlogistics.tracking.repository;

import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.enums.TrackingEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByOrderNumberOrderByTimestampDesc(String orderNumber);

    List<TrackingEvent> findByDriverIdOrderByTimestampDesc(String driverId);

    List<TrackingEvent> findByEventTypeOrderByTimestampDesc(TrackingEventType eventType);

    @Query("SELECT te FROM TrackingEvent te WHERE te.orderNumber = :orderNumber " +
            "AND te.timestamp BETWEEN :startTime AND :endTime ORDER BY te.timestamp DESC")
    List<TrackingEvent> findByOrderNumberAndTimestampBetween(
            @Param("orderNumber") String orderNumber,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT te FROM TrackingEvent te WHERE te.orderNumber = :orderNumber " +
            "AND te.eventType = :eventType ORDER BY te.timestamp DESC")
    List<TrackingEvent> findByOrderNumberAndEventType(
            @Param("orderNumber") String orderNumber,
            @Param("eventType") TrackingEventType eventType);

    @Query("SELECT COUNT(te) FROM TrackingEvent te WHERE te.orderNumber = :orderNumber")
    long countByOrderNumber(@Param("orderNumber") String orderNumber);

    @Query("SELECT COUNT(te) FROM TrackingEvent te")
    long count();
}