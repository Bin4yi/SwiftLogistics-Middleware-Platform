// services/integration-service/src/main/java/com/swiftlogistics/integration/dto/RouteOptimizationResponse.java
package com.swiftlogistics.integration.dto;

public class RouteOptimizationResponse {
    private String orderNumber;
    private String optimizedRoute;
    private Integer estimatedDuration;
    private Double estimatedDistance;
    private String optimizationId;
    private String createdAt;

    // Constructors
    public RouteOptimizationResponse() {}

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getOptimizedRoute() { return optimizedRoute; }
    public void setOptimizedRoute(String optimizedRoute) { this.optimizedRoute = optimizedRoute; }

    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public Double getEstimatedDistance() { return estimatedDistance; }
    public void setEstimatedDistance(Double estimatedDistance) { this.estimatedDistance = estimatedDistance; }

    public String getOptimizationId() { return optimizationId; }
    public void setOptimizationId(String optimizationId) { this.optimizationId = optimizationId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

