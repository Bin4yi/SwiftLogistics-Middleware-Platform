// services/driver-service/src/main/java/com/swiftlogistics/driver/dto/LocationUpdateRequest.java
package com.swiftlogistics.driver.dto;

import javax.validation.constraints.NotNull;

public class LocationUpdateRequest {
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    // Constructors
    public LocationUpdateRequest() {}

    public LocationUpdateRequest(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}