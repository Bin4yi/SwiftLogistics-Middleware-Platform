// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/TrackingUpdateMessage.java

package com.swiftlogistics.tracking.dto;

import java.time.LocalDateTime;

public class TrackingUpdateMessage {
    private String orderNumber;
    private String status;
    private String driverId;
    private String clientId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private String source; // ORDER_SERVICE, DRIVER_SERVICE, INTEGRATION_SERVICE
    private String notes;

    // Constructors
    public TrackingUpdateMessage() {}

    public TrackingUpdateMessage(String orderNumber, String status) {
        this.orderNumber = orderNumber;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "TrackingUpdateMessage{" +
                "orderNumber='" + orderNumber + '\'' +
                ", status='" + status + '\'' +
                ", driverId='" + driverId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
