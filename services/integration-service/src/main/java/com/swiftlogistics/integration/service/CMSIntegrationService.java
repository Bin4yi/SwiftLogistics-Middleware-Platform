// services/integration-service/src/main/java/com/swiftlogistics/integration/service/CMSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CMSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CMSIntegrationService.class);

    @Value("${external-systems.cms.endpoint:http://localhost:8082/mock/cms}")
    private String cmsEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ProcessingResult registerOrder(OrderMessage orderMessage) {
        logger.info("Registering order {} with CMS (SOAP)", orderMessage.getOrderNumber());

        try {
            String soapRequest = createSoapRequest(orderMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    cmsEndpoint + "/registerOrder",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("CMS order registration successful for: {}", orderMessage.getOrderNumber());
                return ProcessingResult.success("Order registered with CMS", response.getBody());
            } else {
                logger.error("CMS order registration failed for: {}", orderMessage.getOrderNumber());
                return ProcessingResult.failure("CMS registration failed", response.getBody());
            }

        } catch (Exception e) {
            logger.error("Error registering order {} with CMS: {}",
                    orderMessage.getOrderNumber(), e.getMessage(), e);
            return ProcessingResult.failure("CMS registration error: " + e.getMessage());
        }
    }

    public ProcessingResult cancelOrder(String orderNumber) {
        logger.info("Cancelling order {} with CMS", orderNumber);

        try {
            String soapRequest = createSoapCancelRequest(orderNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    cmsEndpoint + "/cancelOrder",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("CMS order cancellation successful for: {}", orderNumber);
                return ProcessingResult.success("Order cancelled with CMS", response.getBody());
            } else {
                logger.error("CMS order cancellation failed for: {}", orderNumber);
                return ProcessingResult.failure("CMS cancellation failed", response.getBody());
            }

        } catch (Exception e) {
            logger.error("Error cancelling order {} with CMS: {}", orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("CMS cancellation error: " + e.getMessage());
        }
    }

    private String createSoapRequest(OrderMessage order) {
        StringBuilder soap = new StringBuilder();
        soap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        soap.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        soap.append("<soap:Header/>");
        soap.append("<soap:Body>");
        soap.append("<RegisterOrderRequest>");
        soap.append("<OrderNumber>").append(escapeXml(order.getOrderNumber())).append("</OrderNumber>");
        soap.append("<ClientId>").append(escapeXml(order.getClientId())).append("</ClientId>");
        soap.append("<PickupAddress>").append(escapeXml(order.getPickupAddress())).append("</PickupAddress>");
        soap.append("<DeliveryAddress>").append(escapeXml(order.getDeliveryAddress())).append("</DeliveryAddress>");
        soap.append("<PackageDescription>").append(escapeXml(order.getPackageDescription())).append("</PackageDescription>");
        soap.append("<Priority>").append(escapeXml(order.getPriority())).append("</Priority>");
        soap.append("<ContactNumber>").append(escapeXml(order.getContactNumber())).append("</ContactNumber>");
        soap.append("</RegisterOrderRequest>");
        soap.append("</soap:Body>");
        soap.append("</soap:Envelope>");

        return soap.toString();
    }

    private String createSoapCancelRequest(String orderNumber) {
        StringBuilder soap = new StringBuilder();
        soap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        soap.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        soap.append("<soap:Header/>");
        soap.append("<soap:Body>");
        soap.append("<CancelOrderRequest>");
        soap.append("<OrderNumber>").append(escapeXml(orderNumber)).append("</OrderNumber>");
        soap.append("</CancelOrderRequest>");
        soap.append("</soap:Body>");
        soap.append("</soap:Envelope>");

        return soap.toString();
    }

    /**
     * Basic XML escaping to prevent XML injection
     */
    private String escapeXml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}