// services/integration-service/src/main/java/com/swiftlogistics/integration/controller/CMSMockController.java
package com.swiftlogistics.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/mock/cms")
public class CMSMockController {

    private static final Logger logger = LoggerFactory.getLogger(CMSMockController.class);
    private final Random random = new Random();

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
        return start > 12 && end > start ? soapRequest.substring(start, end) : "UNKNOWN";
    }

    private String createSoapSuccessResponse(String orderNumber, String cmsOrderId) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <RegisterOrderResponse xmlns=\"http://cms.swiftlogistics.com/\">\n" +
                "            <result>SUCCESS</result>\n" +
                "            <OrderNumber>" + orderNumber + "</OrderNumber>\n" +
                "            <CMSOrderId>" + cmsOrderId + "</CMSOrderId>\n" +
                "            <Message>Order registered successfully in CMS</Message>\n" +
                "            <Timestamp>" + LocalDateTime.now() + "</Timestamp>\n" +
                "        </RegisterOrderResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";
    }

    private String createSoapErrorResponse(String error, String orderNumber) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <soap:Fault>\n" +
                "            <faultcode>Client</faultcode>\n" +
                "            <faultstring>" + error + "</faultstring>\n" +
                "            <detail>\n" +
                "                <OrderNumber>" + orderNumber + "</OrderNumber>\n" +
                "            </detail>\n" +
                "        </soap:Fault>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";
    }

    private String createSoapCancelResponse(String orderNumber) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "    <soap:Body>\n" +
                "        <CancelOrderResponse xmlns=\"http://cms.swiftlogistics.com/\">\n" +
                "            <result>SUCCESS</result>\n" +
                "            <OrderNumber>" + orderNumber + "</OrderNumber>\n" +
                "            <Message>Order cancelled successfully</Message>\n" +
                "            <Timestamp>" + LocalDateTime.now() + "</Timestamp>\n" +
                "        </CancelOrderResponse>\n" +
                "    </soap:Body>\n" +
                "</soap:Envelope>";
    }
}