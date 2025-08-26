// services/integration-service/src/main/java/com/swiftlogistics/integration/controller/WMSMockController.java
package com.swiftlogistics.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/mock/wms")
public class WMSMockController {

    private static final Logger logger = LoggerFactory.getLogger(WMSMockController.class);
    private final Random random = new Random();

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

        logger.info("WMS Mock: Package added for order: {} at location: {}", orderNumber, warehouseLocation);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/removePackage", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> removePackage(@RequestBody String tcpMessage) {
        logger.info("WMS Mock: Received TCP package removal request");

        String orderNumber = extractFromTcpMessage(tcpMessage, "ORDER_NUMBER");
        logger.info("WMS Mock: Processing package removal for order: {}", orderNumber);

        // Simulate processing time
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = createTcpSuccessResponse("PACKAGE_REMOVED", orderNumber, "Package removed from warehouse");

        logger.info("WMS Mock: Package removed for order: {}", orderNumber);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/getStatus", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getPackageStatus(@RequestBody String tcpMessage) {
        logger.debug("WMS Mock: Received TCP status request");

        String orderNumber = extractFromTcpMessage(tcpMessage, "ORDER_NUMBER");

        String[] statuses = {"RECEIVED", "IN_STORAGE", "PICKING", "PACKED", "READY_FOR_DISPATCH"};
        String status = statuses[random.nextInt(statuses.length)];

        String response = createTcpSuccessResponse("STATUS_RETRIEVED", orderNumber,
                "Package status: " + status + ", Location: WH-A-" + random.nextInt(50));

        return ResponseEntity.ok(response);
    }

    private String extractFromTcpMessage(String tcpMessage, String field) {
        Pattern pattern = Pattern.compile(field + ":(.*?)\\n");
        Matcher matcher = pattern.matcher(tcpMessage);
        return matcher.find() ? matcher.group(1).trim() : "UNKNOWN";
    }

    private String createTcpSuccessResponse(String command, String orderNumber, String message) {
        return "SWIFT_WMS_PROTOCOL_V1\n" +
                "RESPONSE:SUCCESS\n" +
                "COMMAND:" + command + "\n" +
                "ORDER_NUMBER:" + orderNumber + "\n" +
                "MESSAGE:" + message + "\n" +
                "TIMESTAMP:" + System.currentTimeMillis() + "\n" +
                "SERVER_ID:WMS-SERVER-01\n" +
                "END\n";
    }

    private String createTcpErrorResponse(String command, String orderNumber, String error) {
        return "SWIFT_WMS_PROTOCOL_V1\n" +
                "RESPONSE:ERROR\n" +
                "COMMAND:" + command + "\n" +
                "ORDER_NUMBER:" + orderNumber + "\n" +
                "ERROR:" + error + "\n" +
                "TIMESTAMP:" + System.currentTimeMillis() + "\n" +
                "SERVER_ID:WMS-SERVER-01\n" +
                "END\n";
    }
}