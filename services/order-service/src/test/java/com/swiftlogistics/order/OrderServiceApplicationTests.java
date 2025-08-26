// services/order-service/src/test/java/com/swiftlogistics/order/OrderServiceApplicationTests.java
package com.swiftlogistics.order;

import com.swiftlogistics.order.dto.OrderRequest;
import com.swiftlogistics.order.dto.OrderResponse;
import com.swiftlogistics.order.entity.OrderPriority;
import com.swiftlogistics.order.entity.OrderStatus;
import com.swiftlogistics.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceApplicationTests {

	@Autowired
	private OrderService orderService;

	@Test
	void contextLoads() {
		assertNotNull(orderService);
	}

	@Test
	void testOrderSubmission() {
		// Create test order request
		OrderRequest request = new OrderRequest();
		request.setClientId("CLIENT-001");
		request.setPickupAddress("Test Pickup Address");
		request.setDeliveryAddress("Test Delivery Address");
		request.setPackageDescription("Test Package");
		request.setPriority(OrderPriority.STANDARD);

		// Submit order
		OrderResponse response = orderService.submitOrder(request);

		// Verify response
		assertNotNull(response);
		assertNotNull(response.getOrderNumber());
		assertEquals(OrderStatus.SUBMITTED, response.getStatus());
		assertEquals("CLIENT-001", response.getClientId());
		assertEquals("Test Pickup Address", response.getPickupAddress());
	}

	@Test
	void testOrderRetrieval() {
		// First create an order
		OrderRequest request = new OrderRequest();
		request.setClientId("CLIENT-001");
		request.setPickupAddress("Test Pickup");
		request.setDeliveryAddress("Test Delivery");
		request.setPackageDescription("Test Package");

		OrderResponse created = orderService.submitOrder(request);

		// Retrieve the order
		OrderResponse retrieved = orderService.getOrderByNumber(created.getOrderNumber());

		// Verify
		assertNotNull(retrieved);
		assertEquals(created.getOrderNumber(), retrieved.getOrderNumber());
		assertEquals(created.getClientId(), retrieved.getClientId());
	}

	@Test
	void testOrderStatusUpdate() {
		// Create order
		OrderRequest request = new OrderRequest();
		request.setClientId("CLIENT-001");
		request.setPickupAddress("Test Pickup");
		request.setDeliveryAddress("Test Delivery");
		request.setPackageDescription("Test Package");

		OrderResponse created = orderService.submitOrder(request);

		// Update status
		OrderResponse updated = orderService.updateOrderStatus(
				created.getOrderNumber(), OrderStatus.PROCESSING);

		// Verify
		assertEquals(OrderStatus.PROCESSING, updated.getStatus());
		assertNotNull(updated.getUpdatedAt());
	}
}

