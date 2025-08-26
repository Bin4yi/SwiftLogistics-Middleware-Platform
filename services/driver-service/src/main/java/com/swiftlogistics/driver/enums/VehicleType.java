// services/driver-service/src/main/java/com/swiftlogistics/driver/enums/VehicleType.java
package com.swiftlogistics.driver.enums;

public enum VehicleType {
    MOTORCYCLE("Motorcycle - up to 5kg"),
    BICYCLE("Bicycle - up to 3kg"),
    SCOOTER("Scooter - up to 8kg"),
    CAR("Car - up to 20kg"),
    VAN("Van - up to 100kg"),
    TRUCK("Truck - up to 500kg");

    private final String description;

    VehicleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}