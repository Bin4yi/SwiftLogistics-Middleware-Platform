// services/integration-service/src/main/java/com/swiftlogistics/integration/dto/OrderMessage.java
package com.swiftlogistics.integration.dto;

public class OrderMessage {
    private String orderNumber;
    private String clientId;
    private String pickupAddress;
    private String deliveryAddress;
    private String packageDescription;
    private String priority;

    // Constructors
    public OrderMessage() {}

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

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}