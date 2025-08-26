// services/integration-service/src/main/java/com/swiftlogistics/integration/dto/RouteOptimizationResponse.java
package com.swiftlogistics.integration.dto;

import java.util.List;

public class RouteOptimizationResponse {
    private String orderNumber;
    private String routeId;
    private String estimatedDeliveryTime;
    private Double estimatedDistance;
    private Integer estimatedDuration;
    private String assignedVehicle;
    private String driverId;
    private List<String> routePoints;

    // Constructors
    public RouteOptimizationResponse() {}

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public Double getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(Double estimatedDistance) { this.estimatedDistance = estimatedDistance; }

    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getAssignedVehicle() { return assignedVehicle; }
    public void setAssignedVehicle(String assignedVehicle) { this.assignedVehicle = assignedVehicle; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public List<String> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<String> routePoints) { this.routePoints = routePoints; }
}