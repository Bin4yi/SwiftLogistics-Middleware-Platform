// services/driver-service/src/main/java/com/swiftlogistics/driver/DriverServiceApplication.java
package com.swiftlogistics.driver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableScheduling
public class DriverServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(DriverServiceApplication.class, args);
	}
}