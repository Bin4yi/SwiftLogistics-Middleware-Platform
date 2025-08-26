// services/driver-service/src/main/java/com/swiftlogistics/driver/repository/DeliveryRepository.java
package com.swiftlogistics.driver.repository;

import com.swiftlogistics.driver.entity.Delivery;
import com.swiftlogistics.driver.entity.Driver;
import com.swiftlogistics.driver.enums.DeliveryStatus;
import com.swiftlogistics.driver.enums.OrderPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderNumber(String orderNumber);

    List<Delivery> findByDriver(Driver driver);

    List<Delivery> findByDriverAndStatus(Driver driver, DeliveryStatus status);

    List<Delivery> findByStatus(DeliveryStatus status);

    List<Delivery> findByPriority(OrderPriority priority);

    @Query("SELECT d FROM Delivery d WHERE d.driver.driverId = :driverId ORDER BY d.routeSequence ASC")
    List<Delivery> findByDriverIdOrderByRouteSequence(@Param("driverId") String driverId);

    @Query("SELECT d FROM Delivery d WHERE d.driver.driverId = :driverId AND d.scheduledDate BETWEEN :startDate AND :endDate")
    List<Delivery> findByDriverIdAndDateRange(@Param("driverId") String driverId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Delivery d WHERE d.scheduledDate BETWEEN :startDate AND :endDate")
    List<Delivery> findByScheduledDateBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Delivery d WHERE d.driver = :driver AND d.status IN ('ASSIGNED', 'ACKNOWLEDGED', 'EN_ROUTE_PICKUP', 'AT_PICKUP', 'PICKED_UP', 'EN_ROUTE_DELIVERY', 'AT_DELIVERY') ORDER BY d.priority DESC, d.routeSequence ASC")
    List<Delivery> findActiveDeliveriesByDriver(@Param("driver") Driver driver);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.driver = :driver AND d.status = :status")
    long countByDriverAndStatus(@Param("driver") Driver driver, @Param("status") DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.status = 'ASSIGNED' AND d.driver IS NULL ORDER BY d.priority DESC, d.createdAt ASC")
    List<Delivery> findUnassignedDeliveries();

    @Query("SELECT d FROM Delivery d WHERE d.routeId = :routeId ORDER BY d.routeSequence ASC")
    List<Delivery> findByRouteIdOrderBySequence(@Param("routeId") String routeId);
}