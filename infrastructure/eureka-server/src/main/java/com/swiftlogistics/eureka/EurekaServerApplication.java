// infrastructure/eureka-server/src/main/java/com/swiftlogistics/eureka/EurekaServerApplication.java
package com.swiftlogistics.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server Application for SwiftLogistics
 * Provides service discovery and registration capabilities
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

	public static void main(String[] args) {
		System.out.println("Starting SwiftLogistics Eureka Server...");
		SpringApplication.run(EurekaServerApplication.class, args);
	}
}