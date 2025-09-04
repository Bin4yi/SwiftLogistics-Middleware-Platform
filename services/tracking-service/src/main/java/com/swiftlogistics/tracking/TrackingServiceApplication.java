// services/tracking-service/src/main/java/com/swiftlogistics/tracking/TrackingServiceApplication.java
package com.swiftlogistics.tracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
// REMOVE THIS IMPORT: import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
// REMOVE THIS ANNOTATION: @EnableEurekaClient
@EnableCaching
public class TrackingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingServiceApplication.class, args);
    }
}