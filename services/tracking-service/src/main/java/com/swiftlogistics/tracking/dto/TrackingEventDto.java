// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/TrackingEventDto.java

package com.swiftlogistics.tracking.dto;

import com.swiftlogistics.tracking.enums.TrackingEventType;
import java.time.LocalDateTime;

public class TrackingEventDto {
    private Long id;
    private String orderNumber;
    private TrackingEventType eventType;
    private String eventDescription;
    private String driverId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private String displayName;
    private String details;

    // Constructors
    public TrackingEventDto() {}

    public TrackingEventDto(String orderNumber, TrackingEventType eventType, String eventDescription) {
        this.orderNumber = orderNumber;
        this.eventType = eventType;
        this.eventDescription = eventDescription;
        this.timestamp = LocalDateTime.now();
        this.displayName = eventType.getDisplayName();
        this.details = eventType.getDescription();
    }

    // Helper method to create from entity
    public static TrackingEventDto fromEntity(com.swiftlogistics.tracking.entity.TrackingEvent event) {
        TrackingEventDto dto = new TrackingEventDto();
        dto.setId(event.getId());
        dto.setOrderNumber(event.getOrderNumber());
        dto.setEventType(event.getEventType());
        dto.setEventDescription(event.getEventDescription());
        dto.setDriverId(event.getDriverId());
        dto.setLatitude(event.getLatitude());
        dto.setLongitude(event.getLongitude());
        dto.setTimestamp(event.getTimestamp());
        dto.setDisplayName(event.getEventType().getDisplayName());
        dto.setDetails(event.getEventType().getDescription());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public TrackingEventType getEventType() { return eventType; }
    public void setEventType(TrackingEventType eventType) {
        this.eventType = eventType;
        if (eventType != null) {
            this.displayName = eventType.getDisplayName();
            this.details = eventType.getDescription();
        }
    }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}