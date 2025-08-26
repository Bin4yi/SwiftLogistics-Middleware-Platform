// services/order-service/src/main/java/com/swiftlogistics/order/repository/ClientRepository.java
package com.swiftlogistics.order.repository;

import com.swiftlogistics.order.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByClientId(String clientId);

    Optional<Client> findByEmail(String email);

    List<Client> findByActive(boolean active);

    @Query("SELECT c FROM Client c WHERE c.companyName LIKE %:name%")  // Fixed field name
    List<Client> findByCompanyNameContaining(@Param("name") String name);

    boolean existsByClientId(String clientId);

    boolean existsByEmail(String email);
}