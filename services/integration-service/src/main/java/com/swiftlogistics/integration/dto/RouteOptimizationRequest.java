// services/integration-service/src/main/java/com/swiftlogistics/integration/dto/RouteOptimizationRequest.java
package com.swiftlogistics.integration.dto;

public class RouteOptimizationRequest {
    private String orderNumber;
    private String pickupAddress;
    private String deliveryAddress;
    private String priority;
    private String vehicleType;
    private String timeWindow;

    // Constructors
    public RouteOptimizationRequest() {}

    public RouteOptimizationRequest(String orderNumber, String pickupAddress, String deliveryAddress) {
        this.orderNumber = orderNumber;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
    }

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getTimeWindow() { return timeWindow; }
    public void setTimeWindow(String timeWindow) { this.timeWindow = timeWindow; }
}

