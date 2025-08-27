// services/tracking-service/src/main/java/com/swiftlogistics/tracking/dto/NotificationMessage.java
package com.swiftlogistics.tracking.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationMessage {
    private String type;
    private String recipient;
    private String orderNumber;
    private String title;
    private String message;
    private Map<String, Object> data;
    private LocalDateTime timestamp;

    // Constructors
    public NotificationMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public NotificationMessage(String type, String recipient, String orderNumber, String title, String message) {
        this.type = type;
        this.recipient = recipient;
        this.orderNumber = orderNumber;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}