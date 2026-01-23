package com.tunisia.financial.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tunisia.financial.enumerations.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * User response DTO
 * Used when returning user information to clients
 * Excludes sensitive fields like password
 * 
 * @param id User's unique identifier
 * @param email User's email address
 * @param firstName User's first name
 * @param lastName User's last name
 * @param role User's role
 * @param accountLocked Whether account is locked
 * @param lastLoginAt Timestamp of last login
 * @param createdAt Account creation timestamp
 */
public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    UserRole role,
    boolean accountLocked,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    Instant lastLoginAt,
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    Instant createdAt
) {
    /**
     * Get user's full name
     */
    public String fullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }
}
