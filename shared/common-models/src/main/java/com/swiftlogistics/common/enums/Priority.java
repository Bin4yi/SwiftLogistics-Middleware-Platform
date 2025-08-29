package com.swiftlogistics.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Priority {
    LOW(1, "LOW", "Low priority delivery"),
    NORMAL(2, "NORMAL", "Normal priority delivery"),
    HIGH(3, "HIGH", "High priority delivery"),
    URGENT(4, "URGENT", "Urgent delivery required"),
    CRITICAL(5, "CRITICAL", "Critical/Emergency delivery");

    private final int level;
    private final String code;
    private final String description;

    Priority(int level, String code, String description) {
        this.level = level;
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public static Priority fromCode(String code) {
        for (Priority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Invalid Priority code: " + code);
    }

    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
}