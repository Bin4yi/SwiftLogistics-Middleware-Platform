
// services/order-service/src/main/java/com/swiftlogistics/order/repository/OrderRepository.java
package com.swiftlogistics.order.repository;

import com.swiftlogistics.order.entity.Order;
import com.swiftlogistics.order.entity.OrderStatus;
import com.swiftlogistics.order.entity.OrderPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByClientId(String clientId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPriority(OrderPriority priority);

    List<Order> findByAssignedDriverId(String driverId);

    @Query("SELECT o FROM Order o WHERE o.clientId = :clientId ORDER BY o.createdAt DESC")
    List<Order> findByClientIdOrderByCreatedAtDesc(@Param("clientId") String clientId);

    @Query("SELECT o FROM Order o WHERE o.status IN :statuses")
    List<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.priority = :priority ORDER BY o.createdAt ASC")
    List<Order> findByStatusAndPriorityOrderByCreatedAtAsc(@Param("status") OrderStatus status,
                                                           @Param("priority") OrderPriority priority);

    // Method 2: Find all orders ordered by creation date (most recent first)
    List<Order> findAllByOrderByCreatedAtDesc();

    // Method 3: Find all orders ordered by creation date (oldest first)
    List<Order> findAllByOrderByCreatedAtAsc();

    // Method 4: Custom query for complex sorting
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllOrdersWithPagination(Pageable pageable);


    // Method 6: Find orders by client with pagination
    Page<Order> findByClientId(String clientId, Pageable pageable);

    // Method 7: Count all orders
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();
}
