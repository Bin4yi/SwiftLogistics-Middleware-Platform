// services/integration-service/src/main/java/com/swiftlogistics/integration/repository/IntegrationTransactionRepository.java
package com.swiftlogistics.integration.repository;

import com.swiftlogistics.integration.entity.IntegrationTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationTransactionRepository extends JpaRepository<IntegrationTransaction, Long> {

    Optional<IntegrationTransaction> findByTransactionId(String transactionId);

    Optional<IntegrationTransaction> findByOrderNumber(String orderNumber);

    List<IntegrationTransaction> findByStatus(String status);

    @Query("SELECT t FROM IntegrationTransaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<IntegrationTransaction> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM IntegrationTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT t FROM IntegrationTransaction t WHERE t.status = 'FAILED' AND t.createdAt > :since")
    List<IntegrationTransaction> findFailedTransactionsSince(@Param("since") LocalDateTime since);
}