// services/order-service/src/main/java/com/swiftlogistics/order/dto/OrderRequest.java
package com.swiftlogistics.order.dto;

import com.swiftlogistics.order.entity.OrderPriority;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class OrderRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    @NotBlank(message = "Package description is required")
    private String packageDescription;

    private OrderPriority priority = OrderPriority.STANDARD;

    private String specialInstructions;
    private String contactNumber;
    private Double packageWeight;

    // Constructors
    public OrderRequest() {}

    public OrderRequest(String clientId, String pickupAddress, String deliveryAddress,
                        String packageDescription, OrderPriority priority) {
        this.clientId = clientId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.packageDescription = packageDescription;
        this.priority = priority;
    }

    // Getters and Setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPackageDescription() { return packageDescription; }
    public void setPackageDescription(String packageDescription) { this.packageDescription = packageDescription; }

    public OrderPriority getPriority() { return priority; }
    public void setPriority(OrderPriority priority) { this.priority = priority; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Double getPackageWeight() { return packageWeight; }
    public void setPackageWeight(Double packageWeight) { this.packageWeight = packageWeight; }
}
