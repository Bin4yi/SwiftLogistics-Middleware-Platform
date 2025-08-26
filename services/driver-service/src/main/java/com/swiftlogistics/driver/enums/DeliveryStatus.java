// services/driver-service/src/main/java/com/swiftlogistics/driver/enums/DeliveryStatus.java
package com.swiftlogistics.driver.enums;

public enum DeliveryStatus {
    ASSIGNED("Delivery assigned to driver"),
    ACKNOWLEDGED("Driver acknowledged the delivery"),
    EN_ROUTE_PICKUP("Driver en route to pickup"),
    AT_PICKUP("Driver at pickup location"),
    PICKED_UP("Package picked up"),
    EN_ROUTE_DELIVERY("En route to delivery"),
    AT_DELIVERY("Driver at delivery location"),
    DELIVERED("Package delivered successfully"),
    FAILED("Delivery failed"),
    RETURNED("Package returned to warehouse"),
    CANCELLED("Delivery cancelled");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}