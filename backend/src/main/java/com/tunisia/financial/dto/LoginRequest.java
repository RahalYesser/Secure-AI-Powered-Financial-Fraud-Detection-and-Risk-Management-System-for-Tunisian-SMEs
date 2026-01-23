package com.tunisia.financial.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO
 * Used for user authentication
 * 
 * @param email User's email address
 * @param password User's password
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }
}
