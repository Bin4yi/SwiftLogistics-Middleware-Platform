// services/driver-service/src/main/java/com/swiftlogistics/driver/entity/Delivery.java
package com.swiftlogistics.driver.entity;

import com.swiftlogistics.driver.enums.DeliveryStatus;
import com.swiftlogistics.driver.enums.OrderPriority;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Order number is required")
    @Column(nullable = false, unique = true)
    private String orderNumber;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @NotBlank(message = "Pickup address is required")
    @Column(length = 500)
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    @Column(length = 500)
    private String deliveryAddress;

    @NotBlank(message = "Package description is required")
    @Column(length = 1000)
    private String packageDescription;

    @Enumerated(EnumType.STRING)
    private OrderPriority priority;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private LocalDateTime scheduledDate;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    private String recipientName;
    private String recipientPhone;
    private String specialInstructions;

    private String proofOfDeliveryPhoto;
    private String digitalSignature;
    private String deliveryNotes;
    private String failureReason;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private Integer routeSequence;
    private String routeId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Delivery() {
        this.createdAt = LocalDateTime.now();
        this.status = DeliveryStatus.ASSIGNED;
    }

    public Delivery(String orderNumber, String clientId, String pickupAddress,
                    String deliveryAddress, String packageDescription, OrderPriority priority) {
        this();
        this.orderNumber = orderNumber;
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.packageDescription = packageDescription;
        this.priority = priority;
        this.assignedAt = LocalDateTime.now();
    }

    public void updateStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        switch (newStatus) {
            case PICKED_UP:
                this.pickedUpAt = LocalDateTime.now();
                break;
            case DELIVERED:
                this.deliveredAt = LocalDateTime.now();
                break;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPackageDescription() { return packageDescription; }
    public void setPackageDescription(String packageDescription) { this.packageDescription = packageDescription; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getProofOfDeliveryPhoto() { return proofOfDeliveryPhoto; }
    public void setProofOfDeliveryPhoto(String proofOfDeliveryPhoto) { this.proofOfDeliveryPhoto = proofOfDeliveryPhoto; }

    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public Double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(Double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }

    public Double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(Double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public Integer getRouteSequence() { return routeSequence; }
    public void setRouteSequence(Integer routeSequence) { this.routeSequence = routeSequence; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
