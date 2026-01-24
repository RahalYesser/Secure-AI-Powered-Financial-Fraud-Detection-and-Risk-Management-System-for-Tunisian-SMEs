package com.tunisia.financial.dto.response;

import com.tunisia.financial.enumerations.RiskCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Record representing a comprehensive credit risk report with detailed analysis
 */
public record RiskReport(
        Long reportId,
        Long assessmentId,
        UUID smeUserId,
        String businessName,
        RiskCategory riskCategory,
        double overallRiskScore,
        FinancialMetrics financialMetrics,
        HistoricalAnalysis historicalAnalysis,
        MarketConditions marketConditions,
        List<RiskFactor> keyRiskFactors,
        List<String> recommendations,
        String executiveSummary,
        Instant generatedAt
) {
    /**
     * Nested record for financial metrics
     */
    public record FinancialMetrics(
            BigDecimal debtRatio,
            BigDecimal currentRatio,
            BigDecimal profitMargin,
            BigDecimal cashFlowScore,
            String liquidityStatus
    ) {
    }
    
    /**
     * Nested record for historical analysis
     */
    public record HistoricalAnalysis(
            int yearsAnalyzed,
            String revenueTrend,
            String profitabilityTrend,
            int numberOfDefaults,
            double averagePaymentDelay
    ) {
    }
    
    /**
     * Nested record for market conditions
     */
    public record MarketConditions(
            String industrySector,
            String marketOutlook,
            double sectorRiskScore,
            String economicIndicators
    ) {
    }
    
    /**
     * Nested record for individual risk factors
     */
    public record RiskFactor(
            String factorName,
            RiskCategory severity,
            double impactScore,
            String description
    ) {
    }
    
    /**
     * Check if the report indicates high risk
     */
    public boolean isHighRisk() {
        return riskCategory == RiskCategory.HIGH || riskCategory == RiskCategory.CRITICAL;
    }
    
    /**
     * Get the number of critical risk factors
     */
    public long getCriticalFactorCount() {
        return keyRiskFactors.stream()
                .filter(factor -> factor.severity() == RiskCategory.CRITICAL)
                .count();
    }
}
