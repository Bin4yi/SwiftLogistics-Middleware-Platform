package com.swiftlogistics.common.util;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private ValidationUtil() {
        // Utility class
    }

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    // Phone number pattern (Sri Lankan format)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+94|0)?[1-9]\\d{8}$");

    // Order number pattern
    private static final Pattern ORDER_NUMBER_PATTERN =
            Pattern.compile("^ORD-\\d{10}$");

    // Driver ID pattern
    private static final Pattern DRIVER_ID_PATTERN =
            Pattern.compile("^DRV-\\d{6}$");

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidOrderNumber(String orderNumber) {
        return orderNumber != null && ORDER_NUMBER_PATTERN.matcher(orderNumber).matches();
    }

    public static boolean isValidDriverId(String driverId) {
        return driverId != null && DRIVER_ID_PATTERN.matcher(driverId).matches();
    }

    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidCoordinate(Double coordinate, double min, double max) {
        return coordinate != null && coordinate >= min && coordinate <= max;
    }

    public static boolean isValidLatitude(Double latitude) {
        return isValidCoordinate(latitude, -90.0, 90.0);
    }

    public static boolean isValidLongitude(Double longitude) {
        return isValidCoordinate(longitude, -180.0, 180.0);
    }

    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[<>\"'&]", "");
    }
}