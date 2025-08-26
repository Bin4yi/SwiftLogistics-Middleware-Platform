// services/order-service/src/main/java/com/swiftlogistics/order/entity/Order.java
package com.swiftlogistics.order.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")  // Fixed: was 'n' instead of 'name'
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @NotBlank(message = "Client ID is required")
    @Column(nullable = false)
    private String clientId;

    @NotBlank(message = "Pickup address is required")
    @Column(nullable = false, length = 500)
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @NotBlank(message = "Package description is required")
    @Column(nullable = false, length = 1000)
    private String packageDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private OrderPriority priority;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String estimatedDeliveryTime;
    private String assignedDriverId;
    private String trackingDetails;

    // Constructors
    public Order() {
        this.createdAt = LocalDateTime.now();
    }

    public Order(String clientId, String pickupAddress, String deliveryAddress,
                 String packageDescription, OrderPriority priority) {
        this();
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.packageDescription = packageDescription;
        this.priority = priority != null ? priority : OrderPriority.STANDARD;
        this.status = OrderStatus.SUBMITTED;
        this.orderNumber = generateOrderNumber();
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

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
