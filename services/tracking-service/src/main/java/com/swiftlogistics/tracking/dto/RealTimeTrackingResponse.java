// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/RealTimeTrackingResponse.java

package com.swiftlogistics.tracking.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RealTimeTrackingResponse {
    private String orderNumber;
    private String currentStatus;
    private String clientId;
    private String driverId;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private LocalDateTime estimatedDeliveryTime;
    private List<TrackingEventDto> recentEvents;
    private Map<String, Object> additionalData;
    private boolean realTimeEnabled;

    // Constructors
    public RealTimeTrackingResponse() {}

    public RealTimeTrackingResponse(String orderNumber, String currentStatus) {
        this.orderNumber = orderNumber;
        this.currentStatus = currentStatus;
        this.realTimeEnabled = true;
    }

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }

    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }

    public LocalDateTime getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }

    public LocalDateTime getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(LocalDateTime estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public List<TrackingEventDto> getRecentEvents() { return recentEvents; }
    public void setRecentEvents(List<TrackingEventDto> recentEvents) { this.recentEvents = recentEvents; }

    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    public boolean isRealTimeEnabled() { return realTimeEnabled; }
    public void setRealTimeEnabled(boolean realTimeEnabled) { this.realTimeEnabled = realTimeEnabled; }
}