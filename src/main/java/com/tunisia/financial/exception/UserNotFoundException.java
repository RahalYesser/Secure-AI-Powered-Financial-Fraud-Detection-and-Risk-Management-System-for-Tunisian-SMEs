package com.tunisia.financial.exception;

import java.util.UUID;

/**
 * Exception thrown when a user is not found
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(UUID userId) {
        super("User not found with ID: " + userId);
    }
    
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
