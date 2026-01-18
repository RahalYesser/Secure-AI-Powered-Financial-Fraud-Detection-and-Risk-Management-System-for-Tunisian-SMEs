package com.tunisia.financial.dto;

import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * User registration request DTO
 * Used when creating a new user account
 * 
 * @param email User's email address (used as username)
 * @param password User's password (must meet security requirements)
 * @param firstName User's first name
 * @param lastName User's last name
 * @param role User's role in the system
 */
public record UserRegistrationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @ValidPassword(message = "Password does not meet security requirements")
    String password,
    
    @NotBlank(message = "First name is required")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    String lastName,
    
    @NotNull(message = "Role is required")
    UserRole role
) {
    /**
     * Create a registration request with validated inputs
     */
    public UserRegistrationRequest {
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
