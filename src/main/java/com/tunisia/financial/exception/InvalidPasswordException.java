package com.tunisia.financial.exception;

/**
 * Exception thrown when password validation fails
 */
public class InvalidPasswordException extends RuntimeException {
    
    public InvalidPasswordException(String message) {
        super(message);
    }
    
    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }
}
