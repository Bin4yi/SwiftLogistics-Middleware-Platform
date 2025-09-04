package com.swiftlogistics.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Order Service Routes
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .uri("http://127.0.0.1:8081"))

                // Integration Service Routes
                .route("integration-service", r -> r
                        .path("/api/integration/**")
                        .uri("http://127.0.0.1:8082"))

                // Integration Mock Routes
                .route("integration-mock", r -> r
                        .path("/mock/**")
                        .uri("http://127.0.0.1:8082"))

                // Tracking Service Routes
                .route("tracking-service", r -> r
                        .path("/api/tracking/**")
                        .uri("http://127.0.0.1:8084"))

                // Driver Service Routes
                .route("driver-service", r -> r
                        .path("/api/drivers/**")
                        .uri("http://127.0.0.1:8083"))

                .build();
    }
}
