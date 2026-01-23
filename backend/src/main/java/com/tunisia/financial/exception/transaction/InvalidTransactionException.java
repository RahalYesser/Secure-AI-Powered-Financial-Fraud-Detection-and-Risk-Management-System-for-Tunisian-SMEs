package com.tunisia.financial.exception.transaction;

/**
 * Exception thrown when a transaction is invalid due to business rules or validation failures
 */
public class InvalidTransactionException extends RuntimeException {
    
    public InvalidTransactionException(String message) {
        super(message);
    }
    
    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
