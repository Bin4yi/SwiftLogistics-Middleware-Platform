package com.swiftlogistics.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    SUBMITTED("SUBMITTED", "Order has been submitted"),
    VALIDATED("VALIDATED", "Order has been validated"),
    PROCESSING("PROCESSING", "Order is being processed"),
    ASSIGNED_TO_DRIVER("ASSIGNED_TO_DRIVER", "Order assigned to driver"),
    PICKED_UP("PICKED_UP", "Package picked up from warehouse"),
    IN_TRANSIT("IN_TRANSIT", "Package is in transit"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Package is out for delivery"),
    DELIVERED("DELIVERED", "Package has been delivered"),
    FAILED_DELIVERY("FAILED_DELIVERY", "Delivery attempt failed"),
    RETURNED("RETURNED", "Package returned to sender"),
    CANCELLED("CANCELLED", "Order has been cancelled");

    private final String code;
    private final String description;

    OrderStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus code: " + code);
    }
}