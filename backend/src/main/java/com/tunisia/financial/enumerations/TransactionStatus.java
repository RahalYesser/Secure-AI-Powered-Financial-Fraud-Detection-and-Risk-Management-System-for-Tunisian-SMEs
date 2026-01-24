package com.tunisia.financial.enumerations;

/**
 * Enumeration representing the status of a financial transaction
 */
public enum TransactionStatus {
    /**
     * Transaction is pending processing
     */
    PENDING,
    
    /**
     * Transaction completed successfully
     */
    COMPLETED,
    
    /**
     * Transaction failed due to an error
     */
    FAILED,
    
    /**
     * Transaction flagged as potential fraud
     */
    FRAUD_DETECTED,
    
    /**
     * Transaction cancelled by user or system
     */
    CANCELLED
}
