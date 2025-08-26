// services/integration-service/src/main/java/com/swiftlogistics/integration/config/IntegrationConfig.java
package com.swiftlogistics.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableRetry
@EnableAsync
@EnableScheduling
public class IntegrationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean(name = "integrationTaskExecutor")
    public Executor integrationTaskExecutor() {
        return Executors.newFixedThreadPool(10);
    }
}