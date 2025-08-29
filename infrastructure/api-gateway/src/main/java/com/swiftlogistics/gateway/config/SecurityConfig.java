package com.swiftlogistics.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersChain;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFiltersChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFiltersChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/mock/**").permitAll()

                        // WebSocket connections
                        .pathMatchers("/ws/**").permitAll()

                        // Test endpoints
                        .pathMatchers("/api/*/test/**").permitAll()

                        // All other requests need authentication
                        .anyExchange().permitAll()  // For demo purposes, allowing all
                )
                .build();
    }
}