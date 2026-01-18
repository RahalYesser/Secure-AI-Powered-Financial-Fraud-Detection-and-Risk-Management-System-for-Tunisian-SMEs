package com.tunisia.financial.exception;

/**
 * Exception thrown when a user already exists with the given email
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
    
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
