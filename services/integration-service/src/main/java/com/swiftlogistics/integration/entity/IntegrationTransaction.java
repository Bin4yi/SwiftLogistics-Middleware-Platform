
// services/integration-service/src/main/java/com/swiftlogistics/integration/entity/IntegrationTransaction.java
package com.swiftlogistics.integration.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "integration_transactions")
public class IntegrationTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private String status; // STARTED, COMPLETED, FAILED

    private String cmsStatus; // PROCESSING, COMPLETED, FAILED
    private String rosStatus; // PROCESSING, COMPLETED, FAILED
    private String wmsStatus; // PROCESSING, COMPLETED, FAILED

    @Column(columnDefinition = "TEXT")
    private String cmsResponse;

    @Column(columnDefinition = "TEXT")
    private String rosResponse;

    @Column(columnDefinition = "TEXT")
    private String wmsResponse;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    // Constructors
    public IntegrationTransaction() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCmsStatus() { return cmsStatus; }
    public void setCmsStatus(String cmsStatus) { this.cmsStatus = cmsStatus; }

    public String getRosStatus() { return rosStatus; }
    public void setRosStatus(String rosStatus) { this.rosStatus = rosStatus; }

    public String getWmsStatus() { return wmsStatus; }
    public void setWmsStatus(String wmsStatus) { this.wmsStatus = wmsStatus; }

    public String getCmsResponse() { return cmsResponse; }
    public void setCmsResponse(String cmsResponse) { this.cmsResponse = cmsResponse; }

    public String getRosResponse() { return rosResponse; }
    public void setRosResponse(String rosResponse) { this.rosResponse = rosResponse; }

    public String getWmsResponse() { return wmsResponse; }
    public void setWmsResponse(String wmsResponse) { this.wmsResponse = wmsResponse; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
