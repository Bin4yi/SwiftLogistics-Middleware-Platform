// services/integration-service/src/main/java/com/swiftlogistics/integration/service/CMSIntegrationService.java
package com.swiftlogistics.integration.service;

import com.swiftlogistics.integration.dto.OrderMessage;
import com.swiftlogistics.integration.dto.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CMSIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(CMSIntegrationService.class);

    @Value("${external-systems.cms.endpoint}")
    private String cmsEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ProcessingResult registerOrder(OrderMessage orderMessage) {
        logger.info("Registering order {} with CMS (SOAP)", orderMessage.getOrderNumber());

        try {
            // Create SOAP XML request
            String soapRequest = createSoapRequest(orderMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "registerOrder");

            HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

            // Call mock CMS endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(
                    cmsEndpoint + "/registerOrder", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                logger.info("CMS registration successful for order: {}", orderMessage.getOrderNumber());

                return ProcessingResult.success("Order registered with CMS", responseBody);
            } else {
                logger.error("CMS registration failed with status: {}", response.getStatusCode());
                return ProcessingResult.failure("CMS registration failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error calling CMS for order {}: {}", orderMessage.getOrderNumber(), e.getMessage(), e);
            return ProcessingResult.failure("CMS integration error: " + e.getMessage());
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public ProcessingResult cancelOrder(String orderNumber) {
        logger.info("Cancelling order {} in CMS (compensation)", orderNumber);

        try {
            String soapRequest = createCancelSoapRequest(orderNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.set("SOAPAction", "cancelOrder");

            HttpEntity<String> request = new HttpEntity<>(soapRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    cmsEndpoint + "/cancelOrder", request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("CMS cancellation successful for order: {}", orderNumber);
                return ProcessingResult.success("Order cancelled in CMS", response.getBody());
            } else {
                logger.error("CMS cancellation failed with status: {}", response.getStatusCode());
                return ProcessingResult.failure("CMS cancellation failed: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error cancelling order {} in CMS: {}", orderNumber, e.getMessage(), e);
            return ProcessingResult.failure("CMS cancellation error: " + e.getMessage());
        }
    }

    private String createSoapRequest(OrderMessage orderMessage) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soap:Header/>\n" +
                        "    <soap:Body>\n" +
                        "        <RegisterOrderRequest xmlns=\"http://cms.swiftlogistics.com/\">\n" +
                        "            <OrderNumber>%s</OrderNumber>\n" +
                        "            <ClientId>%s</ClientId>\n" +
                        "            <PickupAddress>%s</PickupAddress>\n" +
                        "            <DeliveryAddress>%s</DeliveryAddress>\n" +
                        "            <PackageDescription>%s</PackageDescription>\n" +
                        "            <Priority>%s</Priority>\n" +
                        "        </RegisterOrderRequest>\n" +
                        "    </soap:Body>\n" +
                        "</soap:Envelope>",
                orderMessage.getOrderNumber(),
                orderMessage.getClientId(),
                orderMessage.getPickupAddress(),
                orderMessage.getDeliveryAddress(),
                orderMessage.getPackageDescription(),
                orderMessage.getPriority()
        );
    }

    private String createCancelSoapRequest(String orderNumber) {
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                        "    <soap:Header/>\n" +
                        "    <soap:Body>\n" +
                        "        <CancelOrderRequest xmlns=\"http://cms.swiftlogistics.com/\">\n" +
                        "            <OrderNumber>%s</OrderNumber>\n" +
                        "        </CancelOrderRequest>\n" +
                        "    </soap:Body>\n" +
                        "</soap:Envelope>",
                orderNumber
        );
    }
}