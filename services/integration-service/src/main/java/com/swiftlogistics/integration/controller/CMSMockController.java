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

@RestController
@RequestMapping("/mock/cms")
public class CMSMockController {

    private static final Logger logger = LoggerFactory.getLogger(CMSMockController.class);
    private final Random random = new Random();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "CMS Mock");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("protocol", "SOAP/XML");
        response.put("endpoints", Arrays.asList("registerOrder", "register-order", "cancelOrder"));
        response.put("note", "Simulates legacy Client Management System");

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/registerOrder", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> registerOrder(@RequestBody String soapRequest) {
        logger.info("CMS Mock: Received SOAP order registration request");

        // Simulate processing time
        try {
            Thread.sleep(1000 + random.nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Extract order number from SOAP request (simple parsing)
        String orderNumber = extractOrderNumber(soapRequest);

        // Simulate 10% failure rate
        if (random.nextInt(10) == 0) {
            logger.warn("CMS Mock: Simulating failure for order: {}", orderNumber);
            return ResponseEntity.badRequest()
                    .body(createSoapErrorResponse("Order validation failed", orderNumber));
        }

        String responseXml = createSoapSuccessResponse(orderNumber, "CMS-" + System.currentTimeMillis());
        logger.info("CMS Mock: Order {} registered successfully", orderNumber);

        return ResponseEntity.ok(responseXml);
    }

    // ADD: Support hyphenated endpoint
    @PostMapping(value = "/register-order", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> registerOrderHyphenated(@RequestBody String soapRequest) {
        logger.info("CMS Mock: Received SOAP order registration request (hyphenated endpoint)");
        return registerOrder(soapRequest);
    }

    @PostMapping(value = "/cancelOrder", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> cancelOrder(@RequestBody String soapRequest) {
        logger.info("CMS Mock: Received SOAP order cancellation request");

        String orderNumber = extractOrderNumber(soapRequest);

        // Simulate processing time
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String responseXml = createSoapCancelResponse(orderNumber);
        logger.info("CMS Mock: Order {} cancelled successfully", orderNumber);

        return ResponseEntity.ok(responseXml);
    }

    private String extractOrderNumber(String soapRequest) {
        // Simple extraction - in real implementation would use proper XML parsing
        int start = soapRequest.indexOf("<OrderNumber>") + 13;
        int end = soapRequest.indexOf("</OrderNumber>");
        return start > 12 && end > start ?
                soapRequest.substring(start, end) : "UNKNOWN";
    }

    private String createSoapSuccessResponse(String orderNumber, String cmsId) {
        StringBuilder soap = new StringBuilder();
        soap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        soap.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        soap.append("<soap:Header/>");
        soap.append("<soap:Body>");
        soap.append("<RegisterOrderResponse>");
        soap.append("<OrderNumber>").append(orderNumber).append("</OrderNumber>");
        soap.append("<Status>SUCCESS</Status>");
        soap.append("<CmsId>").append(cmsId).append("</CmsId>");
        soap.append("<Message>Order registered successfully</Message>");
        soap.append("</RegisterOrderResponse>");
        soap.append("</soap:Body>");
        soap.append("</soap:Envelope>");
        return soap.toString();
    }

    private String createSoapErrorResponse(String error, String orderNumber) {
        StringBuilder soap = new StringBuilder();
        soap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        soap.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        soap.append("<soap:Header/>");
        soap.append("<soap:Body>");
        soap.append("<soap:Fault>");
        soap.append("<faultcode>Client</faultcode>");
        soap.append("<faultstring>").append(error).append("</faultstring>");
        soap.append("<detail>");
        soap.append("<OrderNumber>").append(orderNumber).append("</OrderNumber>");
        soap.append("</detail>");
        soap.append("</soap:Fault>");
        soap.append("</soap:Body>");
        soap.append("</soap:Envelope>");
        return soap.toString();
    }

    private String createSoapCancelResponse(String orderNumber) {
        StringBuilder soap = new StringBuilder();
        soap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        soap.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        soap.append("<soap:Header/>");
        soap.append("<soap:Body>");
        soap.append("<CancelOrderResponse>");
        soap.append("<OrderNumber>").append(orderNumber).append("</OrderNumber>");
        soap.append("<Status>CANCELLED</Status>");
        soap.append("<Message>Order cancelled successfully</Message>");
        soap.append("</CancelOrderResponse>");
        soap.append("</soap:Body>");
        soap.append("</soap:Envelope>");
        return soap.toString();
    }
}