package com.tunisia.financial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration
 * 
 * This configures Cross-Origin Resource Sharing (CORS) to allow the React frontend
 * to communicate with the Spring Boot backend.
 * 
 * Security considerations:
 * - Allows specific origins (localhost development and production domains)
 * - Allows necessary HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
 * - Allows authentication headers (Authorization, Content-Type)
 * - Enables credentials (cookies, authorization headers)
 */
@Configuration
public class CorsConfig {

    /**
     * CORS Configuration Source
     * 
     * This bean is automatically picked up by Spring Security and applied
     * to all endpoints.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from these origins
        // In production, replace with actual frontend domain
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",      // React dev server
            "http://localhost:5173",      // Vite dev server
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://frontend:3000"        // Docker frontend service
        ));
        
        // Allow these HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // Allow these headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Requested-With"
        ));
        
        // Expose these headers to the frontend
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);
        
        // Apply this configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
