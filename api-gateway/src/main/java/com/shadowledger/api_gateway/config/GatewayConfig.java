package com.shadowledger.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Event Service routes
                .route("event-service", r -> r.path("/events/**")
                        .uri("http://localhost:8081"))
                // Shadow Ledger Service routes
                .route("shadow-ledger-service", r -> r.path("/accounts/**")
                        .uri("http://localhost:8082"))
                // Drift Correction Service routes
                .route("drift-correction-service", r -> r.path("/drift-check/**", "/correct/**")
                        .uri("http://localhost:8083"))
                .build();
    }
}
