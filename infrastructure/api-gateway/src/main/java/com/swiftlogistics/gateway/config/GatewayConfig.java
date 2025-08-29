package com.swiftlogistics.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Order Service Routes
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("order-service-cb")
                                        .setFallbackUri("forward:/fallback/order"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET, HttpMethod.POST)
                                        .setBackoff(java.time.Duration.ofMillis(100),
                                                java.time.Duration.ofMillis(1000), 2, true)))
                        .uri("lb://order-service"))

                // Integration Service Routes
                .route("integration-service", r -> r
                        .path("/api/integration/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("integration-service-cb")
                                        .setFallbackUri("forward:/fallback/integration"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://integration-service"))

                // Tracking Service Routes
                .route("tracking-service", r -> r
                        .path("/api/tracking/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("tracking-service-cb")
                                        .setFallbackUri("forward:/fallback/tracking"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://tracking-service"))

                // Driver Service Routes
                .route("driver-service", r -> r
                        .path("/api/drivers/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("driver-service-cb")
                                        .setFallbackUri("forward:/fallback/driver"))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver())))
                        .uri("lb://driver-service"))

                // Authentication Routes (no rate limiting for login)
                .route("auth-routes", r -> r
                        .path("/api/auth/**")
                        .uri("lb://driver-service"))

                // Mock Services for Testing
                .route("cms-mock", r -> r
                        .path("/mock/cms/**")
                        .uri("lb://integration-service"))

                .route("ros-mock", r -> r
                        .path("/mock/ros/**")
                        .uri("lb://integration-service"))

                .route("wms-mock", r -> r
                        .path("/mock/wms/**")
                        .uri("lb://integration-service"))

                // WebSocket Routes for Real-time Tracking
                .route("websocket-tracking", r -> r
                        .path("/ws/**")
                        .uri("lb://tracking-service"))

                .build();
    }

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter redisRateLimiter() {
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.KeyResolver userKeyResolver() {
        return exchange -> reactor.core.publisher.Mono.just("anonymous");
    }
}