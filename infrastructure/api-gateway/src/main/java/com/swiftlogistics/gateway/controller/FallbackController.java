package com.swiftlogistics.gateway.controller;

import com.swiftlogistics.gateway.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/order")
    public ResponseEntity<ApiResponse<String>> orderServiceFallback() {
        logger.warn("Order service is currently unavailable - fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Order service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/integration")
    public ResponseEntity<ApiResponse<String>> integrationServiceFallback() {
        logger.warn("Integration service is currently unavailable - fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Integration service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/tracking")
    public ResponseEntity<ApiResponse<String>> trackingServiceFallback() {
        logger.warn("Tracking service is currently unavailable - fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Tracking service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/driver")
    public ResponseEntity<ApiResponse<String>> driverServiceFallback() {
        logger.warn("Driver service is currently unavailable - fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Driver service is temporarily unavailable. Please try again later."));
    }
}