package com.tunisia.financial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for password encoding
 */
@Configuration
public class SecurityConfig {
    
    /**
     * Password encoder using BCrypt
     * BCrypt is a secure hashing algorithm with built-in salt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength of 12 for security
    }
}
