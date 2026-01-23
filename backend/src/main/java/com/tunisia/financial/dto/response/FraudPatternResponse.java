package com.tunisia.financial.dto.response;

import java.time.Instant;

/**
 * Response DTO for fraud pattern information
 */
public record FraudPatternResponse(
        Long id,
        String patternType,
        String description,
        Double confidence,
        Long transactionId,
        String detectorModel,
        Instant detectedAt,
        Boolean reviewed
) {
}
