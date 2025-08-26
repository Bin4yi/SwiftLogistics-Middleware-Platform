package com.swiftlogistics.integration.dto;

public class ProcessingResult {
    private boolean success;
    private String message;
    private String response;
    private String errorMessage;

    private ProcessingResult() {}

    public static ProcessingResult success(String message, String response) {
        ProcessingResult result = new ProcessingResult();
        result.success = true;
        result.message = message;
        result.response = response;
        return result;
    }

    public static ProcessingResult failure(String errorMessage) {
        ProcessingResult result = new ProcessingResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getResponse() { return response; }
    public String getErrorMessage() { return errorMessage; }
}