// COMPLETE UPDATED WMSMockController.java
package com.swiftlogistics.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/mock/wms")
public class WMSMockController {

    private static final Logger logger = LoggerFactory.getLogger(WMSMockController.class);
    private final Random random = new Random();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "WMS Mock");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("protocol", "TCP/IP (simulated via HTTP)");
        response.put("endpoints", Arrays.asList("addPackage", "add-package", "removePackage"));
        response.put("note", "Simulates Warehouse Management System");

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/addPackage", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addPackage(@RequestBody String tcpMessage) {
        logger.info("WMS Mock: Received TCP package addition request");

        String orderNumber = extractFromTcpMessage(tcpMessage, "ORDER_NUMBER");
        logger.info("WMS Mock: Processing package addition for order: {}", orderNumber);

        // Simulate processing time
        try {
            Thread.sleep(800 + random.nextInt(1500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 8% failure rate
        if (random.nextInt(12) == 0) {
            logger.warn("WMS Mock: Simulating package addition failure for order: {}", orderNumber);
            return ResponseEntity.badRequest()
                    .body(createTcpErrorResponse("PACKAGE_ADDITION_FAILED", orderNumber, "Warehouse capacity exceeded"));
        }

        String warehouseLocation = "WH-" + (char)('A' + random.nextInt(5)) + "-" + (random.nextInt(99) + 1);
        String trackingCode = "TRK-" + System.currentTimeMillis();

        String response = createTcpSuccessResponse("PACKAGE_ADDED", orderNumber,
                "Package added successfully. Location: " + warehouseLocation + ", Tracking: " + trackingCode);

        logger.info("WMS Mock: Package addition successful for order: {} at location: {}", orderNumber, warehouseLocation);
        return ResponseEntity.ok(response);
    }

    // ADD: Support hyphenated endpoint
    @PostMapping(value = "/add-package", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> addPackageHyphenated(@RequestBody String tcpMessage) {
        logger.info("WMS Mock: Received TCP package addition request (hyphenated endpoint)");
        return addPackage(tcpMessage);
    }

    @PostMapping(value = "/removePackage", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> removePackage(@RequestBody String tcpMessage) {
        logger.info("WMS Mock: Received TCP package removal request");

        String orderNumber = extractFromTcpMessage(tcpMessage, "ORDER_NUMBER");
        logger.info("WMS Mock: Processing package removal for order: {}", orderNumber);

        // Simulate processing time
        try {
            Thread.sleep(300 + random.nextInt(700));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = createTcpSuccessResponse("PACKAGE_REMOVED", orderNumber, "Package removed successfully");
        logger.info("WMS Mock: Package removal successful for order: {}", orderNumber);
        return ResponseEntity.ok(response);
    }

    private String extractFromTcpMessage(String tcpMessage, String field) {
        Pattern pattern = Pattern.compile(field + ":([^\\n]+)");
        Matcher matcher = pattern.matcher(tcpMessage);
        return matcher.find() ? matcher.group(1).trim() : "UNKNOWN";
    }

    private String createTcpSuccessResponse(String operation, String orderNumber, String message) {
        return String.format("STATUS:SUCCESS\nOPERATION:%s\nORDER_NUMBER:%s\nMESSAGE:%s\nTIMESTAMP:%s\nEND\n",
                operation, orderNumber, message, LocalDateTime.now());
    }

    private String createTcpErrorResponse(String operation, String orderNumber, String error) {
        return String.format("STATUS:ERROR\nOPERATION:%s\nORDER_NUMBER:%s\nERROR:%s\nTIMESTAMP:%s\nEND\n",
                operation, orderNumber, error, LocalDateTime.now());
    }
}