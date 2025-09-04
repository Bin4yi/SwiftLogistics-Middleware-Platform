// services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/TrackingResponse.java
package com.swiftlogistics.tracking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class TrackingResponse implements Serializable {

    private String orderNumber;
    private String currentStatus;
    private String clientId;
    private String assignedDriverId;
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLocationUpdate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private List<TrackingEventDto> trackingHistory;

    // Constructors
    public TrackingResponse() {}

    public TrackingResponse(String orderNumber, String currentStatus, String clientId) {
        this.orderNumber = orderNumber;
        this.currentStatus = currentStatus;
        this.clientId = clientId;
    }

    // Getters and Setters
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAssignedDriverId() {
        return assignedDriverId;
    }

    public void setAssignedDriverId(String assignedDriverId) {
        this.assignedDriverId = assignedDriverId;
    }

    public Double getLastKnownLatitude() {
        return lastKnownLatitude;
    }

    public void setLastKnownLatitude(Double lastKnownLatitude) {
        this.lastKnownLatitude = lastKnownLatitude;
    }

    public Double getLastKnownLongitude() {
        return lastKnownLongitude;
    }

    public void setLastKnownLongitude(Double lastKnownLongitude) {
        this.lastKnownLongitude = lastKnownLongitude;
    }

    public LocalDateTime getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) {
        this.lastLocationUpdate = lastLocationUpdate;
    }

    public LocalDateTime getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TrackingEventDto> getTrackingHistory() {
        return trackingHistory;
    }

    public void setTrackingHistory(List<TrackingEventDto> trackingHistory) {
        this.trackingHistory = trackingHistory;
    }

    // Helper methods
    public boolean hasLocationData() {
        return lastKnownLatitude != null && lastKnownLongitude != null;
    }

    public boolean isActive() {
        return currentStatus != null &&
                !currentStatus.equals("DELIVERED") &&
                !currentStatus.equals("CANCELLED") &&
                !currentStatus.equals("FAILED");
    }

    public int getEventCount() {
        return trackingHistory != null ? trackingHistory.size() : 0;
    }

    @Override
    public String toString() {
        return "TrackingResponse{" +
                "orderNumber='" + orderNumber + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", clientId='" + clientId + '\'' +
                ", assignedDriverId='" + assignedDriverId + '\'' +
                ", hasLocation=" + hasLocationData() +
                ", eventCount=" + getEventCount() +
                ", isActive=" + isActive() +
                '}';
    }
}