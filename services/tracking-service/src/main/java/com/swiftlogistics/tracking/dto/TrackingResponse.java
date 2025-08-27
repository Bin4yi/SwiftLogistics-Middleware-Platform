// services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/TrackingResponse.java
package com.swiftlogistics.tracking.dto;

import com.swiftlogistics.tracking.entity.TrackingEvent;

import java.time.LocalDateTime;
import java.util.List;

public class TrackingResponse {
    private String orderNumber;
    private String currentStatus;
    private String assignedDriverId;
    private LocalDateTime estimatedDeliveryTime;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private List<TrackingEvent> trackingHistory;
    private String statusDescription;

    // Constructors
    public TrackingResponse() {}

    public TrackingResponse(String orderNumber, String currentStatus) {
        this.orderNumber = orderNumber;
        this.currentStatus = currentStatus;
    }

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public LocalDateTime getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) {
        this.lastLocationUpdate = lastLocationUpdate;
    }

    public List<TrackingEvent> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<TrackingEvent> trackingHistory) { this.trackingHistory = trackingHistory; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }
}