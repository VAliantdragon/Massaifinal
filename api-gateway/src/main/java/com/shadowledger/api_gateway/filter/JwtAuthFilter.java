package com.shadowledger.api_gateway.filter;

import com.shadowledger.api_gateway.service.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Skip authentication for /auth/token and actuator endpoints
        if (path.equals("/auth/token") || path.startsWith("/actuator")) {
            return addTraceId(exchange, chain);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Missing Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            // Validate JWT token
            Claims claims = jwtService.validateToken(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            // Check if token is expired
            if (jwtService.isTokenExpired(token)) {
                return sendUnauthorized(exchange, "Token expired");
            }

            // Check RBAC permissions
            if (!hasPermission(path, role)) {
                return sendForbidden(exchange, "Insufficient permissions for role: " + role);
            }

            // Add user info and trace ID to headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Trace-Id", UUID.randomUUID().toString())
                    .header("X-User", username)
                    .header("X-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return sendUnauthorized(exchange, "Invalid JWT token: " + e.getMessage());
        }
    }

    private boolean hasPermission(String path, String role) {
        if (role == null) return false;

        // RBAC Rules as per requirements
        if (path.startsWith("/events")) {
            return "USER".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/drift-check")) {
            return "AUDITOR".equals(role) || "ADMIN".equals(role);
        }
        if (path.startsWith("/correct")) {
            return "ADMIN".equals(role);
        }

        return false;
    }

    private Mono<Void> addTraceId(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-Trace-Id", UUID.randomUUID().toString())
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"status\":401}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    private Mono<Void> sendForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");

        String body = "{\"error\":\"Forbidden\",\"message\":\"" + message + "\",\"status\":403}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // High priority to run before routing
    }
}
