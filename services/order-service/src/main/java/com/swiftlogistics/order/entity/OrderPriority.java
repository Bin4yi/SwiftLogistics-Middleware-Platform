// services/order-service/src/main/java/com/swiftlogistics/order/entity/OrderPriority.java
package com.swiftlogistics.order.entity;

public enum OrderPriority {
    STANDARD(1, "Standard delivery"),
    EXPRESS(2, "Express delivery - next day"),
    URGENT(3, "Urgent delivery - same day");

    private final int level;
    private final String description;

    OrderPriority(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() { return level; }
    public String getDescription() { return description; }
}