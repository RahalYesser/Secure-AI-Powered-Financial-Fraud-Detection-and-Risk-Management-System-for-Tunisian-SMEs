package com.tunisia.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Record representing financial data for credit risk assessment
 * Contains comprehensive financial indicators for SME businesses
 */
public record FinancialData(
        @NotNull(message = "SME user ID is required")
        UUID smeUserId,
        
        @NotNull(message = "Annual revenue is required")
        @DecimalMin(value = "0.0", message = "Annual revenue must be non-negative")
        BigDecimal annualRevenue,
        
        @NotNull(message = "Total assets are required")
        @DecimalMin(value = "0.0", message = "Total assets must be non-negative")
        BigDecimal totalAssets,
        
        @NotNull(message = "Total liabilities are required")
        @DecimalMin(value = "0.0", message = "Total liabilities must be non-negative")
        BigDecimal totalLiabilities,
        
        @NotNull(message = "Monthly cash flow is required")
        BigDecimal monthlyCashFlow,
        
        @NotNull(message = "Outstanding debt is required")
        @DecimalMin(value = "0.0", message = "Outstanding debt must be non-negative")
        BigDecimal outstandingDebt,
        
        @NotNull(message = "Number of employees is required")
        @PositiveOrZero(message = "Number of employees must be non-negative")
        Integer numberOfEmployees,
        
        @NotNull(message = "Years in business is required")
        @PositiveOrZero(message = "Years in business must be non-negative")
        Integer yearsInBusiness,
        
        @NotNull(message = "Industry sector is required")
        String industrySector,
        
        @NotNull(message = "Credit history score is required")
        @DecimalMin(value = "0.0", message = "Credit history score must be non-negative")
        @DecimalMin(value = "0.0", message = "Credit history score must be between 0 and 100")
        Double creditHistoryScore,
        
        @NotNull(message = "Number of late payments is required")
        @PositiveOrZero(message = "Number of late payments must be non-negative")
        Integer numberOfLatePayments,
        
        BigDecimal currentRatio,
        BigDecimal debtToEquityRatio,
        BigDecimal profitMargin,
        
        @NotNull(message = "Assessment date is required")
        Instant assessmentDate
) {
    /**
     * Calculate debt ratio
     */
    public double calculateDebtRatio() {
        if (totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return totalLiabilities.divide(totalAssets, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Calculate asset to revenue ratio
     */
    public double calculateAssetToRevenueRatio() {
        if (annualRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return totalAssets.divide(annualRevenue, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }
    
    /**
     * Check if business is established (more than 2 years)
     */
    public boolean isEstablished() {
        return yearsInBusiness >= 2;
    }
    
    /**
     * Calculate monthly debt service ratio
     */
    public double calculateMonthlyDebtServiceRatio() {
        if (monthlyCashFlow.compareTo(BigDecimal.ZERO) <= 0) {
            return Double.MAX_VALUE;
        }
        BigDecimal monthlyDebtPayment = outstandingDebt.divide(
                BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
        return monthlyDebtPayment.divide(monthlyCashFlow, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
