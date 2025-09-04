// File: services/tracking-service/src/main/java/com/swiftlogistics/tracking/config/WebSocketConfig.java

package com.swiftlogistics.tracking.config;

import com.swiftlogistics.tracking.websocket.TrackingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TrackingWebSocketHandler trackingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(trackingWebSocketHandler, "/ws/tracking")
                .setAllowedOrigins("*")  // Allow all origins for development
                .withSockJS();           // Enable SockJS fallback

        // Also register without SockJS for native WebSocket clients
        registry.addHandler(trackingWebSocketHandler, "/websocket/tracking")
                .setAllowedOrigins("*");
    }
}