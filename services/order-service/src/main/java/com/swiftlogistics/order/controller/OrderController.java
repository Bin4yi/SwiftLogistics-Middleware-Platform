// services/order-service/src/main/java/com/swiftlogistics/order/controller/OrderController.java
package com.swiftlogistics.order.controller;

import com.swiftlogistics.order.dto.ApiResponse;
import com.swiftlogistics.order.dto.OrderRequest;
import com.swiftlogistics.order.dto.OrderResponse;
import com.swiftlogistics.order.entity.OrderStatus;
import com.swiftlogistics.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(@Valid @RequestBody OrderRequest request) {
        logger.info("Received order submission request for client: {}", request.getClientId());

        try {
            OrderResponse order = orderService.submitOrder(request);
            logger.info("Order submitted successfully: {}", order.getOrderNumber());

            return ResponseEntity.ok(ApiResponse.success("Order submitted successfully", order));

        } catch (Exception e) {
            logger.error("Error submitting order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to submit order: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable String orderNumber) {
        logger.debug("Fetching order: {}", orderNumber);

        try {
            OrderResponse order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(ApiResponse.success(order));

        } catch (Exception e) {
            logger.error("Error fetching order {}: {}", orderNumber, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getClientOrders(@PathVariable String clientId) {
        logger.debug("Fetching orders for client: {}", clientId);

        try {
            List<OrderResponse> orders = orderService.getOrdersByClient(clientId);
            return ResponseEntity.ok(ApiResponse.success(orders));

        } catch (Exception e) {
            logger.error("Error fetching orders for client {}: {}", clientId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch orders: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderNumber}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam OrderStatus status) {
        logger.info("Updating order {} status to {}", orderNumber, status);

        try {
            OrderResponse order = orderService.updateOrderStatus(orderNumber, status);
            return ResponseEntity.ok(ApiResponse.success("Status updated successfully", order));

        } catch (Exception e) {
            logger.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderNumber}/assign-driver")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDriver(
            @PathVariable String orderNumber,
            @RequestParam String driverId) {
        logger.info("Assigning driver {} to order {}", driverId, orderNumber);

        try {
            OrderResponse order = orderService.assignDriverToOrder(orderNumber, driverId);
            return ResponseEntity.ok(ApiResponse.success("Driver assigned successfully", order));

        } catch (Exception e) {
            logger.error("Error assigning driver: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to assign driver: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(@PathVariable OrderStatus status) {
        logger.debug("Fetching orders with status: {}", status);

        try {
            List<OrderResponse> orders = orderService.getOrdersByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(orders));

        } catch (Exception e) {
            logger.error("Error fetching orders by status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDriver(@PathVariable String driverId) {
        logger.debug("Fetching orders for driver: {}", driverId);

        try {
            List<OrderResponse> orders = orderService.getOrdersByDriver(driverId);
            return ResponseEntity.ok(ApiResponse.success(orders));

        } catch (Exception e) {
            logger.error("Error fetching orders for driver: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch orders: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> searchOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        try {
            if (startDate != null && endDate != null) {
                List<OrderResponse> orders = orderService.getOrdersByDateRange(startDate, endDate);
                return ResponseEntity.ok(ApiResponse.success(orders));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Start date and end date are required"));
            }

        } catch (Exception e) {
            logger.error("Error searching orders: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to search orders: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getOrderStats() {
        logger.debug("Fetching order statistics");

        try {
            Map<String, Long> stats = Map.of(
                    "submitted", orderService.getOrderCountByStatus(OrderStatus.SUBMITTED),
                    "processing", orderService.getOrderCountByStatus(OrderStatus.PROCESSING),
                    "in_transit", orderService.getOrderCountByStatus(OrderStatus.IN_TRANSIT),
                    "delivered", orderService.getOrderCountByStatus(OrderStatus.DELIVERED)
            );

            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            logger.error("Error fetching order statistics: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "order-service",
                "timestamp", LocalDateTime.now(),
                "version", "1.0.0"
        ));
    }
}
