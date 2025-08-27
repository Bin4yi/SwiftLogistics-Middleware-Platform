// services/tracking-service/src/main/java/com/swiftlogistics/tracking/entity/TrackingEvent.java
package com.swiftlogistics.tracking.entity;

import com.swiftlogistics.tracking.enums.TrackingEventType;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TrackingEventType eventType;

    @Column(name = "event_description")
    private String eventDescription;

    @Column(name = "driver_id")
    private String driverId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // Constructors
    public TrackingEvent() {}

    public TrackingEvent(String orderNumber, TrackingEventType eventType, String eventDescription) {
        this.orderNumber = orderNumber;
        this.eventType = eventType;
        this.eventDescription = eventDescription;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public TrackingEventType getEventType() { return eventType; }
    public void setEventType(TrackingEventType eventType) { this.eventType = eventType; }

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

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}