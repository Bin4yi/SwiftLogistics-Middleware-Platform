// services/tracking-service/src/main/java/com/swiftlogistics/tracking/TrackingServiceApplication.java
package com.swiftlogistics.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TrackingServiceApplication {

    public static void main(String[] args) {
        // Disable Spring Cloud auto-configuration
        System.setProperty("spring.cloud.config.enabled", "false");
        System.setProperty("spring.cloud.discovery.enabled", "false");
        System.setProperty("eureka.client.enabled", "false");
        
        SpringApplication app = new SpringApplication(TrackingServiceApplication.class);
        
        // Exclude all Spring Cloud auto-configurations
        app.setAdditionalProfiles("no-cloud");
        
        app.run(args);
    }
}