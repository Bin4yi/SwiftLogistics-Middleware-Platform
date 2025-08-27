// services/tracking-service/src/test/java/com/swiftlogistics/tracking/service/TrackingServiceTest.java
package com.swiftlogistics.tracking.service;

import com.swiftlogistics.tracking.dto.TrackingResponse;
import com.swiftlogistics.tracking.entity.DeliveryTracking;
import com.swiftlogistics.tracking.entity.TrackingEvent;
import com.swiftlogistics.tracking.repository.DeliveryTrackingRepository;
import com.swiftlogistics.tracking.repository.TrackingEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @Mock
    private DeliveryTrackingRepository deliveryTrackingRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TrackingService trackingService;

    @BeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetOrderTracking_Success() {
        // Arrange
        String orderNumber = "ORD-001";
        DeliveryTracking tracking = new DeliveryTracking(orderNumber, "CLIENT-001", "IN_TRANSIT");
        tracking.setAssignedDriverId("DRV-001");

        TrackingEvent event = new TrackingEvent(orderNumber, TrackingEventType.ORDER_CREATED, "Order created");

        when(valueOperations.get(any())).thenReturn(null);
        when(deliveryTrackingRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(tracking));
        when(trackingEventRepository.findByOrderNumberOrderByTimestampDesc(orderNumber))
                .thenReturn(Arrays.asList(event));

        // Act
        TrackingResponse result = trackingService.getOrderTracking(orderNumber);

        // Assert
        assertNotNull(result);
        assertEquals(orderNumber, result.getOrderNumber());
        assertEquals("IN_TRANSIT", result.getCurrentStatus());
        assertEquals("DRV-001", result.getAssignedDriverId());
        assertEquals(1, result.getTrackingHistory().size());
    }

    @Test
    void testCreateOrderTracking_Success() {
        // Arrange
        String orderNumber = "ORD-002";
        String clientId = "CLIENT-001";
        String status = "CREATED";

        when(deliveryTrackingRepository.save(any(DeliveryTracking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingEventRepository.save(any(TrackingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        trackingService.createOrderTracking(orderNumber, clientId, status);

        // Assert
        verify(deliveryTrackingRepository).save(any(DeliveryTracking.class));
        verify(trackingEventRepository).save(any(TrackingEvent.class));
        verify(redisTemplate).delete(any(String.class));
    }

    @Test
    void testUpdateOrderStatus_Success() {
        // Arrange
        String orderNumber = "ORD-001";
        String newStatus = "DELIVERED";
        String driverId = "DRV-001";

        DeliveryTracking tracking = new DeliveryTracking(orderNumber, "CLIENT-001", "IN_TRANSIT");

        when(deliveryTrackingRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(tracking));
        when(deliveryTrackingRepository.save(any(DeliveryTracking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingEventRepository.save(any(TrackingEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        trackingService.updateOrderStatus(orderNumber, newStatus, driverId, "Package delivered successfully");

        // Assert
        verify(deliveryTrackingRepository).save(any(DeliveryTracking.class));
        verify(trackingEventRepository).save(any(TrackingEvent.class));
        verify(redisTemplate).delete(any(String.class));
        assertEquals(newStatus, tracking.getCurrentStatus());
        assertEquals(driverId, tracking.getAssignedDriverId());
    }
}