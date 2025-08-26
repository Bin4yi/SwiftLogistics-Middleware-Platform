// services/order-service/src/main/java/com/swiftlogistics/order/entity/OrderStatus.java
package com.swiftlogistics.order.entity;

public enum OrderStatus {
    SUBMITTED("Order has been submitted"),
    PROCESSING("Order is being processed"),
    CMS_CONFIRMED("CMS has confirmed the order"),
    ROUTE_OPTIMIZED("Route has been optimized"),
    WAREHOUSE_READY("Package is ready in warehouse"),
    READY_FOR_DELIVERY("Ready for delivery"),
    ASSIGNED_TO_DRIVER("Assigned to driver"),
    IN_TRANSIT("Package is in transit"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Package has been delivered"),
    FAILED("Delivery failed"),
    CANCELLED("Order has been cancelled");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
