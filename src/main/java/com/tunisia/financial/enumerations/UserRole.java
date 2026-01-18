package com.tunisia.financial.enumerations;

/**
 * User roles for the financial fraud detection system
 * Defines different access levels and permissions for system users
 * 
 * ADMIN: Full system access, user management, and configuration
 * FINANCIAL_ANALYST: Fraud analysis, risk assessment, and reporting
 * SME_USER: SME business owners accessing their own financial data
 * AUDITOR: Read-only access for compliance and audit purposes
 */
public enum UserRole {
    /**
     * Administrator role with full system privileges
     * Can manage users, configure system settings, and access all features
     */
    ADMIN,
    
    /**
     * Financial analyst role for fraud detection and risk assessment
     * Can analyze transactions, generate reports, and manage fraud cases
     */
    FINANCIAL_ANALYST,
    
    /**
     * SME user role for business owners
     * Can view their own transactions, risk scores, and financial reports
     */
    SME_USER,
    
    /**
     * Auditor role for compliance and oversight
     * Read-only access to transaction logs, audit trails, and reports
     */
    AUDITOR
}
