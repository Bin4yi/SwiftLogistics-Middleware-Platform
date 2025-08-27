package com.swiftlogistics.tracking.enums;

public enum TrackingEventType {
    ORDER_CREATED("Order Created", "Order has been created and submitted"),
    ORDER_CONFIRMED("Order Confirmed", "Order has been confirmed and is being processed"),
    ORDER_ASSIGNED_TO_DRIVER("Assigned to Driver", "Order has been assigned to a driver"),
    ORDER_PICKED_UP("Picked Up", "Order has been picked up from warehouse"),
    ORDER_IN_TRANSIT("In Transit", "Order is in transit to destination"),
    ORDER_OUT_FOR_DELIVERY("Out for Delivery", "Order is out for delivery"),
    ORDER_DELIVERED("Delivered", "Order has been successfully delivered"),
    ORDER_FAILED("Delivery Failed", "Delivery attempt failed"),
    ORDER_CANCELLED("Cancelled", "Order has been cancelled"),
    DRIVER_ASSIGNED("Driver Assigned", "Driver has been assigned to the order"),
    DRIVER_EN_ROUTE("Driver En Route", "Driver is en route to pickup/delivery location"),
    DRIVER_ARRIVED("Driver Arrived", "Driver has arrived at the location"),
    LOCATION_UPDATE("Location Update", "Driver location has been updated"),
    STATUS_UPDATE("Status Update", "Order status has been updated"),
    NOTIFICATION_SENT("Notification Sent", "Notification has been sent"),
    EXCEPTION_OCCURRED("Exception Occurred", "An exception or error has occurred");

    private final String displayName;
    private final String description;

    TrackingEventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}