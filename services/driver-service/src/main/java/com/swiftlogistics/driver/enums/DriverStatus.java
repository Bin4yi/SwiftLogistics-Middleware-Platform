// services/driver-service/src/main/java/com/swiftlogistics/driver/enums/DriverStatus.java
package com.swiftlogistics.driver.enums;

public enum DriverStatus {
    OFFLINE("Driver is offline"),
    AVAILABLE("Driver is available for deliveries"),
    BUSY("Driver is currently on delivery"),
    ON_BREAK("Driver is on break"),
    UNAVAILABLE("Driver is unavailable"),
    SUSPENDED("Driver is suspended");

    private final String description;

    DriverStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
