// services/order-service/src/main/java/com/swiftlogistics/order/config/DatabaseConfig.java
package com.swiftlogistics.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableJpaRepositories(basePackages = "com.swiftlogistics.order.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // Database configuration is handled by Spring Boot auto-configuration
    // This class is for any custom database configurations if needed
}


