package com.swiftlogistics.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String correlationId = UUID.randomUUID().toString();
        String method = request.getMethod().toString();
        String url = request.getURI().toString();
        String remoteAddress = request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        // Add correlation ID to headers
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Correlation-ID", correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        long startTime = System.currentTimeMillis();

        logger.info("Gateway Request: [{}] {} {} from {} at {}",
                correlationId, method, url, remoteAddress, LocalDateTime.now());

        return chain.filter(mutatedExchange)
                .doOnSuccess(aVoid -> {
                    long endTime = System.currentTimeMillis();
                    logger.info("Gateway Response: [{}] {} {} - Status: {} - Duration: {}ms",
                            correlationId, method, url,
                            response.getStatusCode(), endTime - startTime);
                })
                .doOnError(throwable -> {
                    long endTime = System.currentTimeMillis();
                    logger.error("Gateway Error: [{}] {} {} - Error: {} - Duration: {}ms",
                            correlationId, method, url,
                            throwable.getMessage(), endTime - startTime);
                });
    }

    @Override
    public int getOrder() {
        return -1; // High priority
    }
}