package com.tunisia.financial.dto;

import com.tunisia.financial.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Password change request DTO
 * Used when a user wants to change their password
 * 
 * @param currentPassword User's current password
 * @param newPassword New password (must meet security requirements)
 */
public record PasswordChangeRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,
    
    @NotBlank(message = "New password is required")
    @ValidPassword(message = "New password does not meet security requirements")
    String newPassword
) {}
