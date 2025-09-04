// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/websocket/WebSocketSessionManager.java

package com.swiftlogistics.tracking.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    @Autowired
    private ObjectMapper objectMapper;

    // Active WebSocket sessions
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Subscription mappings
    private final Map<String, Set<String>> orderSubscriptions = new ConcurrentHashMap<>();      // orderNumber -> sessionIds
    private final Map<String, Set<String>> driverSubscriptions = new ConcurrentHashMap<>();     // driverId -> sessionIds
    private final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();     // clientId -> sessionIds

    // Session tracking (for cleanup)
    private final Map<String, Set<String>> sessionOrderSubs = new ConcurrentHashMap<>();        // sessionId -> orderNumbers
    private final Map<String, Set<String>> sessionDriverSubs = new ConcurrentHashMap<>();       // sessionId -> driverIds
    private final Map<String, Set<String>> sessionClientSubs = new ConcurrentHashMap<>();       // sessionId -> clientIds

    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        logger.debug("Added WebSocket session: {} (Total active: {})", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);

        // Clean up all subscriptions for this session
        cleanupSessionSubscriptions(sessionId);

        logger.debug("Removed WebSocket session: {} (Total active: {})", sessionId, sessions.size());
    }

    private void cleanupSessionSubscriptions(String sessionId) {
        // Clean up order subscriptions
        Set<String> orderNumbers = sessionOrderSubs.remove(sessionId);
        if (orderNumbers != null) {
            orderNumbers.forEach(orderNumber -> {
                Set<String> subscribers = orderSubscriptions.get(orderNumber);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        orderSubscriptions.remove(orderNumber);
                    }
                }
            });
        }

        // Clean up driver subscriptions
        Set<String> driverIds = sessionDriverSubs.remove(sessionId);
        if (driverIds != null) {
            driverIds.forEach(driverId -> {
                Set<String> subscribers = driverSubscriptions.get(driverId);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        driverSubscriptions.remove(driverId);
                    }
                }
            });
        }

        // Clean up client subscriptions
        Set<String> clientIds = sessionClientSubs.remove(sessionId);
        if (clientIds != null) {
            clientIds.forEach(clientId -> {
                Set<String> subscribers = clientSubscriptions.get(clientId);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        clientSubscriptions.remove(clientId);
                    }
                }
            });
        }
    }

    public void subscribeToOrder(WebSocketSession session, String orderNumber) {
        String sessionId = session.getId();

        // Add to order subscriptions
        orderSubscriptions.computeIfAbsent(orderNumber, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Track session subscriptions for cleanup
        sessionOrderSubs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(orderNumber);

        logger.debug("Session {} subscribed to order {} (Total order subscribers: {})",
                sessionId, orderNumber, orderSubscriptions.get(orderNumber).size());
    }

    public void subscribeToDriver(WebSocketSession session, String driverId) {
        String sessionId = session.getId();

        // Add to driver subscriptions
        driverSubscriptions.computeIfAbsent(driverId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Track session subscriptions for cleanup
        sessionDriverSubs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(driverId);

        logger.debug("Session {} subscribed to driver {} (Total driver subscribers: {})",
                sessionId, driverId, driverSubscriptions.get(driverId).size());
    }

    public void subscribeToClient(WebSocketSession session, String clientId) {
        String sessionId = session.getId();

        // Add to client subscriptions
        clientSubscriptions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Track session subscriptions for cleanup
        sessionClientSubs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(clientId);

        logger.debug("Session {} subscribed to client {} (Total client subscribers: {})",
                sessionId, clientId, clientSubscriptions.get(clientId).size());
    }

    public boolean unsubscribe(WebSocketSession session, String target, String targetId) {
        String sessionId = session.getId();
        boolean success = false;

        switch (target.toLowerCase()) {
            case "order":
                success = unsubscribeFromOrder(sessionId, targetId);
                break;
            case "driver":
                success = unsubscribeFromDriver(sessionId, targetId);
                break;
            case "client":
                success = unsubscribeFromClient(sessionId, targetId);
                break;
            default:
                logger.warn("Unknown unsubscribe target: {}", target);
        }

        return success;
    }

    private boolean unsubscribeFromOrder(String sessionId, String orderNumber) {
        Set<String> subscribers = orderSubscriptions.get(orderNumber);
        Set<String> sessionOrders = sessionOrderSubs.get(sessionId);

        boolean removed = false;
        if (subscribers != null) {
            removed = subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                orderSubscriptions.remove(orderNumber);
            }
        }

        if (sessionOrders != null) {
            sessionOrders.remove(orderNumber);
        }

        return removed;
    }

    private boolean unsubscribeFromDriver(String sessionId, String driverId) {
        Set<String> subscribers = driverSubscriptions.get(driverId);
        Set<String> sessionDrivers = sessionDriverSubs.get(sessionId);

        boolean removed = false;
        if (subscribers != null) {
            removed = subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                driverSubscriptions.remove(driverId);
            }
        }

        if (sessionDrivers != null) {
            sessionDrivers.remove(driverId);
        }

        return removed;
    }

    private boolean unsubscribeFromClient(String sessionId, String clientId) {
        Set<String> subscribers = clientSubscriptions.get(clientId);
        Set<String> sessionClients = sessionClientSubs.get(sessionId);

        boolean removed = false;
        if (subscribers != null) {
            removed = subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                clientSubscriptions.remove(clientId);
            }
        }

        if (sessionClients != null) {
            sessionClients.remove(clientId);
        }

        return removed;
    }

    // Real-time broadcasting methods
    public void broadcastOrderUpdate(String orderNumber, Object update) {
        Set<String> subscribers = orderSubscriptions.get(orderNumber);
        if (subscribers != null && !subscribers.isEmpty()) {
            Map<String, Object> message = Map.of(
                    "type", "order_update",
                    "orderNumber", orderNumber,
                    "data", update,
                    "timestamp", System.currentTimeMillis()
            );

            int delivered = broadcastToSessions(subscribers, message);
            logger.debug("Broadcasted order update for {}: {}/{} sessions reached",
                    orderNumber, delivered, subscribers.size());
        }
    }

    public void broadcastDriverUpdate(String driverId, Object update) {
        Set<String> subscribers = driverSubscriptions.get(driverId);
        if (subscribers != null && !subscribers.isEmpty()) {
            Map<String, Object> message = Map.of(
                    "type", "driver_update",
                    "driverId", driverId,
                    "data", update,
                    "timestamp", System.currentTimeMillis()
            );

            int delivered = broadcastToSessions(subscribers, message);
            logger.debug("Broadcasted driver update for {}: {}/{} sessions reached",
                    driverId, delivered, subscribers.size());
        }
    }

    public void broadcastClientUpdate(String clientId, Object update) {
        Set<String> subscribers = clientSubscriptions.get(clientId);
        if (subscribers != null && !subscribers.isEmpty()) {
            Map<String, Object> message = Map.of(
                    "type", "client_update",
                    "clientId", clientId,
                    "data", update,
                    "timestamp", System.currentTimeMillis()
            );

            int delivered = broadcastToSessions(subscribers, message);
            logger.debug("Broadcasted client update for {}: {}/{} sessions reached",
                    clientId, delivered, subscribers.size());
        }
    }

    private int broadcastToSessions(Set<String> sessionIds, Object message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            logger.error("Error serializing broadcast message: {}", e.getMessage(), e);
            return 0;
        }

        int successCount = 0;

        // Use removeIf to clean up dead sessions during broadcast
        sessionIds.removeIf(sessionId -> {
            WebSocketSession session = sessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                return true; // Remove dead session
            }

            try {
                session.sendMessage(new TextMessage(jsonMessage));
                return false; // Keep active session
            } catch (Exception e) {
                logger.error("Error sending message to session {}: {}", sessionId, e.getMessage());
                return true; // Remove problematic session
            }
        });

        return successCount;
    }

    // Status methods
    public Map<String, Object> getConnectionStats() {
        return Map.of(
                "activeSessions", sessions.size(),
                "orderSubscriptions", orderSubscriptions.size(),
                "driverSubscriptions", driverSubscriptions.size(),
                "clientSubscriptions", clientSubscriptions.size(),
                "totalOrderSubscribers", orderSubscriptions.values().stream().mapToInt(Set::size).sum(),
                "totalDriverSubscribers", driverSubscriptions.values().stream().mapToInt(Set::size).sum(),
                "totalClientSubscribers", clientSubscriptions.values().stream().mapToInt(Set::size).sum()
        );
    }

    public boolean hasActiveConnections() {
        return !sessions.isEmpty();
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}