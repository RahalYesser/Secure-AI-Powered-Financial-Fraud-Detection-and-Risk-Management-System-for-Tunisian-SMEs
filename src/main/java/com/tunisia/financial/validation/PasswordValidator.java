package com.tunisia.financial.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for @ValidPassword annotation
 * Enforces password strength requirements
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null passwords are handled by @NotNull annotation
        if (password == null) {
            return true;
        }
        
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();
        
        // Check minimum length
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                "Password must be at least " + minLength + " characters long"
            ).addConstraintViolation();
            return false;
        }
        
        // Check uppercase requirement
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one uppercase letter"
            ).addConstraintViolation();
            return false;
        }
        
        // Check lowercase requirement
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one lowercase letter"
            ).addConstraintViolation();
            return false;
        }
        
        // Check digit requirement
        if (requireDigit && !password.matches(".*\\d.*")) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one digit"
            ).addConstraintViolation();
            return false;
        }
        
        // Check special character requirement
        if (requireSpecialChar && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one special character (!@#$%^&*()_+-=[]{};\\':\"|,.<>/?)"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
