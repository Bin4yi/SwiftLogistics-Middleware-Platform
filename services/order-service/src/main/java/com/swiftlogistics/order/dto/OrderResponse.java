// services/order-service/src/main/java/com/swiftlogistics/order/dto/OrderResponse.java
package com.swiftlogistics.order.dto;

import com.swiftlogistics.order.entity.OrderStatus;
import com.swiftlogistics.order.entity.OrderPriority;
import java.time.LocalDateTime;

public class OrderResponse {
    private String orderNumber;
    private String clientId;
    private String pickupAddress;
    private String deliveryAddress;
    private String packageDescription;
    private OrderStatus status;
    private OrderPriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String estimatedDeliveryTime;
    private String assignedDriverId;
    private String trackingDetails;

    // Constructors
    public OrderResponse() {}

    public OrderResponse(String orderNumber, OrderStatus status, String message) {
        this.orderNumber = orderNumber;
        this.status = status;
    }

    // Getters and Setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPackageDescription() { return packageDescription; }
    public void setPackageDescription(String packageDescription) { this.packageDescription = packageDescription; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }

    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public String getTrackingDetails() { return trackingDetails; }
    public void setTrackingDetails(String trackingDetails) { this.trackingDetails = trackingDetails; }
}