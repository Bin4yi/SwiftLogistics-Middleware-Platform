package com.swiftlogistics.notification.service;

import com.swiftlogistics.notification.dto.NotificationRequest;
import com.swiftlogistics.notification.dto.NotificationResponse;
import com.swiftlogistics.notification.entity.Notification;
import com.swiftlogistics.notification.enums.NotificationType;
import com.swiftlogistics.notification.enums.NotificationStatus;
import com.swiftlogistics.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SMSService smsService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Async
    public CompletableFuture<NotificationResponse> sendNotification(NotificationRequest request) {
        logger.info("Sending notification: type={}, recipient={}",
                request.getType(), request.getRecipient());

        Notification notification = new Notification();
        notification.setType(request.getType());
        notification.setRecipient(request.getRecipient());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setMetadata(request.getMetadata());

        try {
            boolean sent = false;

            switch (request.getType()) {
                case EMAIL:
                    sent = emailService.sendEmail(
                            request.getRecipient(),
                            request.getSubject(),
                            request.getMessage(),
                            request.getTemplate(),
                            request.getMetadata()
                    );
                    break;

                case SMS:
                    sent = smsService.sendSMS(
                            request.getRecipient(),
                            request.getMessage()
                    );
                    break;

                case PUSH:
                    sent = pushNotificationService.sendPushNotification(
                            request.getRecipient(),
                            request.getSubject(),
                            request.getMessage(),
                            request.getMetadata()
                    );
                    break;

                default:
                    logger.warn("Unsupported notification type: {}", request.getType());
            }

            notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
            notification.setSentAt(sent ? LocalDateTime.now() : null);

        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        notification = notificationRepository.save(notification);

        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setStatus(notification.getStatus());
        response.setSentAt(notification.getSentAt());
        response.setErrorMessage(notification.getErrorMessage());

        return CompletableFuture.completedFuture(response);
    }

    public void sendOrderConfirmation(String clientEmail, String orderNumber, String orderDetails) {
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.EMAIL);
        request.setRecipient(clientEmail);
        request.setSubject("Order Confirmation - " + orderNumber);
        request.setTemplate("order-confirmation");
        request.getMetadata().put("orderNumber", orderNumber);
        request.getMetadata().put("orderDetails", orderDetails);

        sendNotification(request);
    }

    public void sendDeliveryUpdate(String clientEmail, String clientPhone, String orderNumber, String status) {
        // Send email notification
        NotificationRequest emailRequest = new NotificationRequest();
        emailRequest.setType(NotificationType.EMAIL);
        emailRequest.setRecipient(clientEmail);
        emailRequest.setSubject("Delivery Update - " + orderNumber);
        emailRequest.setTemplate("delivery-update");
        emailRequest.getMetadata().put("orderNumber", orderNumber);
        emailRequest.getMetadata().put("status", status);

        sendNotification(emailRequest);

        // Send SMS notification for important updates
        if ("DELIVERED".equals(status) || "OUT_FOR_DELIVERY".equals(status)) {
            NotificationRequest smsRequest = new NotificationRequest();
            smsRequest.setType(NotificationType.SMS);
            smsRequest.setRecipient(clientPhone);
            smsRequest.setMessage("SwiftLogistics: Your order " + orderNumber + " is " + status.toLowerCase().replace("_", " "));

            sendNotification(smsRequest);
        }
    }

    public void sendDriverNotification(String driverId, String title, String message) {
        NotificationRequest request = new NotificationRequest();
        request.setType(NotificationType.PUSH);
        request.setRecipient(driverId);
        request.setSubject(title);
        request.setMessage(message);

        sendNotification(request);
    }
}