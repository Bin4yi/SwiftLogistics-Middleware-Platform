//// services/driver-service/src/main/java/com/swiftlogistics/driver/config/WebConfig.java
//package com.swiftlogistics.driver.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/api/**")
//                // OPTION 1: Use allowedOriginPatterns with credentials (current setup)
//                .allowedOriginPatterns("*")
//                .allowCredentials(true)
//
//                // OPTION 2: Use specific origins with credentials (recommended for production)
//                // .allowedOrigins(
//                //     "http://localhost:3000",    // React client portal
//                //     "http://localhost:3001",    // React driver mobile
//                //     "http://localhost:8080"     // API Gateway
//                // )
//                // .allowCredentials(true)
//
//                // OPTION 3: Use allowedOrigins with "*" but disable credentials
//                // .allowedOrigins("*")
//                // .allowCredentials(false)
//
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
//                .allowedHeaders("*")
//                .maxAge(3600);
//    }
//}