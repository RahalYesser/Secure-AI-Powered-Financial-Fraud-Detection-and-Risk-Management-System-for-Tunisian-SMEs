package com.tunisia.financial.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for password strength requirements
 * Ensures passwords meet security standards for the financial system
 * 
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
    
    String message() default "Password must be at least 8 characters long and contain uppercase, lowercase, digit, and special character";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum password length
     */
    int minLength() default 8;
    
    /**
     * Require at least one uppercase letter
     */
    boolean requireUppercase() default true;
    
    /**
     * Require at least one lowercase letter
     */
    boolean requireLowercase() default true;
    
    /**
     * Require at least one digit
     */
    boolean requireDigit() default true;
    
    /**
     * Require at least one special character
     */
    boolean requireSpecialChar() default true;
}
