package com.swiftlogistics.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    // REMOVED RouteLocator bean - using YAML configuration instead
    // This eliminates any programmatic DNS resolution issues

    public GatewayConfig() {
        logger.info("API Gateway configured with direct routing (DNS bypass)");
    }
}
