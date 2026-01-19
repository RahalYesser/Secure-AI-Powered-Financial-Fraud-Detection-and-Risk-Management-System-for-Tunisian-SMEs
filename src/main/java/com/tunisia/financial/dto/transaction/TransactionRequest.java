package com.tunisia.financial.dto.transaction;

import com.tunisia.financial.enumerations.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a transaction
 */
public record TransactionRequest(
        @NotNull(message = "Transaction type is required")
        TransactionType type,
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,
        
        String description
) {
}
