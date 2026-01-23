package com.tunisia.financial.dto;

import com.tunisia.financial.enumerations.UserRole;
import jakarta.validation.constraints.Email;

/**
 * User update request DTO
 * Used when updating user information
 * All fields are optional
 * 
 * @param firstName Updated first name
 * @param lastName Updated last name
 * @param email Updated email address
 * @param role Updated role (admin only)
 */
public record UserUpdateRequest(
    String firstName,
    String lastName,
    
    @Email(message = "Invalid email format")
    String email,
    
    UserRole role
) {
    /**
     * Create an update request with validated inputs
     */
    public UserUpdateRequest {
        // Trim whitespace from string inputs
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
    }
}
