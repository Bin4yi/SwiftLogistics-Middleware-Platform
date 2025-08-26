//services/order-service/src/main/java/com/swiftlogistics/order/service/messaging/OrderMessageProducer.java
package com.swiftlogistics.order.service.messaging;

import com.swiftlogistics.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchanges.order:order.exchange}")
    private String orderExchange;

    public void sendOrderForProcessing(Order order) {
        logger.info("Sending order {} to processing queue", order.getOrderNumber());

        Map<String, Object> orderData = createOrderMessage(order);

        try {
            rabbitTemplate.convertAndSend(orderExchange, "order.process", orderData);
            logger.info("Order {} sent successfully to processing queue", order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to send order {} to processing queue: {}",
                    order.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to send order for processing", e);
        }
    }

    public void sendStatusUpdate(Order order) {
        logger.debug("Sending status update for order {}: {}",
                order.getOrderNumber(), order.getStatus());

        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("orderNumber", order.getOrderNumber());
        statusUpdate.put("clientId", order.getClientId());
        statusUpdate.put("status", order.getStatus().toString());
        statusUpdate.put("timestamp", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);
        statusUpdate.put("driverId", order.getAssignedDriverId());

        try {
            rabbitTemplate.convertAndSend(orderExchange, "order.status.update", statusUpdate);
            logger.debug("Status update sent for order {}", order.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to send status update for order {}: {}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }

    private Map<String, Object> createOrderMessage(Order order) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderNumber", order.getOrderNumber());
        message.put("clientId", order.getClientId());
        message.put("pickupAddress", order.getPickupAddress());
        message.put("deliveryAddress", order.getDeliveryAddress());
        message.put("packageDescription", order.getPackageDescription());
        message.put("priority", order.getPriority() != null ? order.getPriority().toString() : "STANDARD");
        message.put("status", order.getStatus().toString());
        // Convert LocalDateTime to String to avoid Jackson serialization issues
        message.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        message.put("updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);
        return message;
    }
}