package com.tunisia.financial.exception;

/**
 * Exception thrown when invalid financial data is provided
 */
public class InvalidFinancialDataException extends RuntimeException {
    
    public InvalidFinancialDataException(String message) {
        super(message);
    }
    
    public InvalidFinancialDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
