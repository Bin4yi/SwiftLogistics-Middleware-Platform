
// services/integration-service/src/main/java/com/swiftlogistics/integration/IntegrationServiceApplication.java
package com.swiftlogistics.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableEurekaClient
@EnableRetry
public class IntegrationServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(IntegrationServiceApplication.class, args);
	}
}