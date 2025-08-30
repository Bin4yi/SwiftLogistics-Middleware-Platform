package com.swiftlogistics.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// REMOVED @EnableEurekaClient to avoid DNS issues
public class ApiGatewayApplication {
	public static void main(String[] args) {
		// Set system properties to prevent DNS issues
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.awt.headless", "true");

		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}