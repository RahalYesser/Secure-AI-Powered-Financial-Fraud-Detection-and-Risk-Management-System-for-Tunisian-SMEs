package com.tunisia.financial.dto.transaction;

import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for transaction data
 */
public record TransactionResponse(
        Long id,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        UUID userId,
        String userEmail,
        String description,
        Double fraudScore,
        String referenceNumber,
        String receipt,
        Instant createdAt,
        Instant updatedAt
) {
}
