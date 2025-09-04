// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/websocket/TrackingWebSocketHandler.java

package com.swiftlogistics.tracking.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TrackingWebSocketHandler.class);

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        sessionManager.addSession(session);

        // Send welcome message
        Map<String, Object> welcome = Map.of(
                "type", "connection_established",
                "sessionId", session.getId(),
                "timestamp", System.currentTimeMillis(),
                "message", "Connected to SwiftLogistics real-time tracking"
        );

        String welcomeJson = objectMapper.writeValueAsString(welcome);
        session.sendMessage(new TextMessage(welcomeJson));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) messageData.get("type");

            if (type == null) {
                sendErrorMessage(session, "Message type is required");
                return;
            }

            switch (type.toLowerCase()) {
                case "subscribe_order":
                    handleOrderSubscription(session, messageData);
                    break;

                case "subscribe_driver":
                    handleDriverSubscription(session, messageData);
                    break;

                case "subscribe_client":
                    handleClientSubscription(session, messageData);
                    break;

                case "unsubscribe":
                    handleUnsubscribe(session, messageData);
                    break;

                case "ping":
                    handlePing(session);
                    break;

                default:
                    logger.warn("Unknown message type: {} from session: {}", type, session.getId());
                    sendErrorMessage(session, "Unknown message type: " + type);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message from session {}: {}", session.getId(), e.getMessage(), e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    private void handleOrderSubscription(WebSocketSession session, Map<String, Object> messageData) throws Exception {
        String orderNumber = (String) messageData.get("orderNumber");
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            sendErrorMessage(session, "Order number is required for order subscription");
            return;
        }

        sessionManager.subscribeToOrder(session, orderNumber.trim());

        Map<String, Object> response = Map.of(
                "type", "subscription_confirmed",
                "target", "order",
                "orderNumber", orderNumber,
                "timestamp", System.currentTimeMillis()
        );

        String responseJson = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(responseJson));

        logger.info("Session {} subscribed to order: {}", session.getId(), orderNumber);
    }

    private void handleDriverSubscription(WebSocketSession session, Map<String, Object> messageData) throws Exception {
        String driverId = (String) messageData.get("driverId");
        if (driverId == null || driverId.trim().isEmpty()) {
            sendErrorMessage(session, "Driver ID is required for driver subscription");
            return;
        }

        sessionManager.subscribeToDriver(session, driverId.trim());

        Map<String, Object> response = Map.of(
                "type", "subscription_confirmed",
                "target", "driver",
                "driverId", driverId,
                "timestamp", System.currentTimeMillis()
        );

        String responseJson = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(responseJson));

        logger.info("Session {} subscribed to driver: {}", session.getId(), driverId);
    }

    private void handleClientSubscription(WebSocketSession session, Map<String, Object> messageData) throws Exception {
        String clientId = (String) messageData.get("clientId");
        if (clientId == null || clientId.trim().isEmpty()) {
            sendErrorMessage(session, "Client ID is required for client subscription");
            return;
        }

        sessionManager.subscribeToClient(session, clientId.trim());

        Map<String, Object> response = Map.of(
                "type", "subscription_confirmed",
                "target", "client",
                "clientId", clientId,
                "timestamp", System.currentTimeMillis()
        );

        String responseJson = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(responseJson));

        logger.info("Session {} subscribed to client: {}", session.getId(), clientId);
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> messageData) throws Exception {
        String target = (String) messageData.get("target");
        String targetId = (String) messageData.get("targetId");

        if (target == null || targetId == null) {
            sendErrorMessage(session, "Both target and targetId are required for unsubscribe");
            return;
        }

        boolean success = sessionManager.unsubscribe(session, target, targetId);

        Map<String, Object> response = Map.of(
                "type", "unsubscribe_confirmed",
                "target", target,
                "targetId", targetId,
                "success", success,
                "timestamp", System.currentTimeMillis()
        );

        String responseJson = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(responseJson));

        logger.info("Session {} unsubscribed from {} {}: {}", session.getId(), target, targetId, success);
    }

    private void handlePing(WebSocketSession session) throws Exception {
        Map<String, Object> pong = Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        );

        String pongJson = objectMapper.writeValueAsString(pong);
        session.sendMessage(new TextMessage(pongJson));
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                    "type", "error",
                    "message", errorMessage,
                    "timestamp", System.currentTimeMillis()
            );

            String errorJson = objectMapper.writeValueAsString(error);
            session.sendMessage(new TextMessage(errorJson));
        } catch (Exception e) {
            logger.error("Failed to send error message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {} - Status: {}", session.getId(), status);
        sessionManager.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        sessionManager.removeSession(session);

        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (Exception e) {
                logger.error("Error closing WebSocket session {}: {}", session.getId(), e.getMessage());
            }
        }
    }
}