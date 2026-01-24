package com.tunisia.financial.dto.transaction;

import com.tunisia.financial.enumerations.TransactionStatus;
import com.tunisia.financial.enumerations.TransactionType;

import java.math.BigDecimal;

/**
 * Statistics DTO for transaction data
 */
public record TransactionStatistics(
        Long totalTransactions,
        Long pendingTransactions,
        Long completedTransactions,
        Long failedTransactions,
        Long fraudDetectedTransactions,
        Long cancelledTransactions,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        Long paymentCount,
        Long transferCount,
        Long withdrawalCount,
        Long depositCount
) {
}
