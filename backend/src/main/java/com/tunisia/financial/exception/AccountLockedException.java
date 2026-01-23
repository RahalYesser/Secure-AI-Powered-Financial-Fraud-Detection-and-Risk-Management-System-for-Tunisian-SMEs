package com.tunisia.financial.exception;

import java.time.LocalDateTime;

/**
 * Exception thrown when an account is locked
 */
public class AccountLockedException extends RuntimeException {
    
    private final LocalDateTime lockedUntil;
    
    public AccountLockedException(String message) {
        super(message);
        this.lockedUntil = null;
    }
    
    public AccountLockedException(String message, LocalDateTime lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }
    
    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
