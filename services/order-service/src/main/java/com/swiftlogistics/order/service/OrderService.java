// services/order-service/src/main/java/com/swiftlogistics/order/service/OrderService.java
package com.swiftlogistics.order.service;

import com.swiftlogistics.order.dto.OrderRequest;
import com.swiftlogistics.order.dto.OrderResponse;
import com.swiftlogistics.order.entity.Order;
import com.swiftlogistics.order.entity.OrderStatus;
import com.swiftlogistics.order.entity.OrderPriority;
import com.swiftlogistics.order.repository.OrderRepository;
import com.swiftlogistics.order.service.messaging.OrderMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMessageProducer messageProducer;

    @Autowired
    private ClientService clientService;

    public OrderResponse submitOrder(OrderRequest request) {
        logger.info("Submitting new order for client: {}", request.getClientId());

        try {
            // Validate client exists
            clientService.validateClient(request.getClientId());

            // Create new order
            Order order = new Order(
                    request.getClientId(),
                    request.getPickupAddress(),
                    request.getDeliveryAddress(),
                    request.getPackageDescription(),
                    request.getPriority()
            );

            // Save to database
            order = orderRepository.save(order);
            logger.info("Order created with number: {}", order.getOrderNumber());

            // Send to processing queue
            messageProducer.sendOrderForProcessing(order);

            return mapToOrderResponse(order);

        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to submit order: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        logger.debug("Fetching order: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByClient(String clientId) {
        logger.debug("Fetching orders for client: {}", clientId);

        List<Order> orders = orderRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus status) {
        logger.info("Updating order {} status to {}", orderNumber, status);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        order.updateStatus(status);
        order = orderRepository.save(order);

        // Send status update notification
        messageProducer.sendStatusUpdate(order);

        return mapToOrderResponse(order);
    }

    public OrderResponse assignDriverToOrder(String orderNumber, String driverId) {
        logger.info("Assigning driver {} to order {}", driverId, orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        order.setAssignedDriverId(driverId);
        order.updateStatus(OrderStatus.ASSIGNED_TO_DRIVER);
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        logger.debug("Fetching orders with status: {}", status);

        List<Order> orders = orderRepository.findByStatus(status);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDriver(String driverId) {
        logger.debug("Fetching orders for driver: {}", driverId);

        List<Order> orders = orderRepository.findByAssignedDriverId(driverId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse updateTrackingDetails(String orderNumber, String trackingDetails) {
        logger.info("Updating tracking details for order: {}", orderNumber);

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        order.setTrackingDetails(trackingDetails);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        return mapToOrderResponse(order);
    }

    public long getOrderCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Fetching orders between {} and {}", startDate, endDate);

        List<Order> orders = orderRepository.findByCreatedAtBetween(startDate, endDate);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderNumber(order.getOrderNumber());
        response.setClientId(order.getClientId());
        response.setPickupAddress(order.getPickupAddress());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setPackageDescription(order.getPackageDescription());
        response.setStatus(order.getStatus());
        response.setPriority(order.getPriority());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
        response.setAssignedDriverId(order.getAssignedDriverId());
        response.setTrackingDetails(order.getTrackingDetails());
        return response;
    }
}