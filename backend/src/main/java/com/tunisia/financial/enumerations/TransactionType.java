package com.tunisia.financial.enumerations;

/**
 * Enumeration representing different types of financial transactions
 */
public enum TransactionType {
    /**
     * Payment transaction (e.g., bill payment, purchase)
     */
    PAYMENT,
    
    /**
     * Transfer transaction between accounts
     */
    TRANSFER,
    
    /**
     * Withdrawal transaction (cash out)
     */
    WITHDRAWAL,
    
    /**
     * Deposit transaction (cash in)
     */
    DEPOSIT
}
