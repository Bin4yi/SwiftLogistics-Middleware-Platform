// services/integration-service/src/main/java/com/swiftlogistics/integration/dto/ProcessingResult.java
package com.swiftlogistics.integration.dto;

public class ProcessingResult {
    private boolean success;
    private String message;
    private String details;
    private String response; // Additional field for external system responses
    private String errorMessage; // Specific error message field
    private Long timestamp;

    // Constructors
    public ProcessingResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ProcessingResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
        if (!success) {
            this.errorMessage = message;
        }
    }

    public ProcessingResult(boolean success, String message, String details) {
        this(success, message);
        this.details = details;
        this.response = details; // For backward compatibility
    }

    // Static factory methods
    public static ProcessingResult success(String message) {
        return new ProcessingResult(true, message);
    }

    public static ProcessingResult success(String message, String details) {
        return new ProcessingResult(true, message, details);
    }

    public static ProcessingResult failure(String message) {
        return new ProcessingResult(false, message);
    }

    public static ProcessingResult failure(String message, String details) {
        ProcessingResult result = new ProcessingResult(false, message, details);
        result.setErrorMessage(message);
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
        this.response = details; // Keep response field updated
    }

    public String getResponse() {
        return response != null ? response : details;
    }

    public void setResponse(String response) {
        this.response = response;
        if (this.details == null) {
            this.details = response;
        }
    }

    public String getErrorMessage() {
        return errorMessage != null ? errorMessage : (success ? null : message);
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}