package com.tunisia.financial.ai.risk;

import com.tunisia.financial.dto.FinancialData;
import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.RiskCategory;
import com.tunisia.financial.repository.CreditRiskRepository;
import com.tunisia.financial.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Analyzer for historical financial data and risk patterns
 * Provides insights based on past performance and trends
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HistoricalRiskAnalyzer {
    
    private final CreditRiskRepository creditRiskRepository;
    private final TransactionRepository transactionRepository;
    
    /**
     * Analyze historical risk trend for a user
     * 
     * @param user the SME user to analyze
     * @return analysis summary
     */
    public HistoricalAnalysis analyzeRiskTrend(User user) {
        log.debug("Analyzing historical risk trend for user {}", user.getId());
        
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);
        List<CreditRiskAssessment> historicalAssessments = 
                creditRiskRepository.findBySmeUserOrderByAssessedAtDesc(user);
        
        return new HistoricalAnalysis(
                historicalAssessments.size(),
                calculateRiskTrend(historicalAssessments),
                countDeteriorations(historicalAssessments),
                calculateAverageRiskScore(historicalAssessments)
        );
    }
    
    /**
     * Analyze transaction patterns for risk indicators
     * 
     * @param user the SME user
     * @param months number of months to analyze
     * @return transaction analysis
     */
    public TransactionPatternAnalysis analyzeTransactionPatterns(User user, int months) {
        log.debug("Analyzing transaction patterns for user {} over {} months", user.getId(), months);
        
        Instant cutoffDate = Instant.now().minus(months * 30L, ChronoUnit.DAYS);
        List<Transaction> recentTransactions = 
                transactionRepository.findByUserIdAndCreatedAtAfter(user.getId(), cutoffDate);
        
        return new TransactionPatternAnalysis(
                recentTransactions.size(),
                calculateAverageTransactionAmount(recentTransactions),
                calculateTotalVolume(recentTransactions),
                countLargeTransactions(recentTransactions),
                detectIrregularPatterns(recentTransactions)
        );
    }
    
    /**
     * Generate trend recommendations based on historical data
     * 
     * @param financialData current financial data
     * @param historicalAnalysis historical analysis results
     * @return list of recommendations
     */
    public List<String> generateRecommendations(FinancialData financialData, HistoricalAnalysis historicalAnalysis) {
        List<String> recommendations = new java.util.ArrayList<>();
        
        // Debt management recommendations
        double debtRatio = financialData.calculateDebtRatio();
        if (debtRatio > 0.7) {
            recommendations.add("Prioritize debt reduction - current debt ratio is high at " + 
                    String.format("%.1f%%", debtRatio * 100));
        }
        
        // Cash flow recommendations
        if (financialData.monthlyCashFlow().compareTo(BigDecimal.ZERO) <= 0) {
            recommendations.add("Critical: Improve cash flow management - currently negative");
        } else if (financialData.calculateMonthlyDebtServiceRatio() > 0.4) {
            recommendations.add("Reduce debt service burden - currently consuming significant portion of cash flow");
        }
        
        // Business stability recommendations
        if (financialData.yearsInBusiness() < 2) {
            recommendations.add("Focus on establishing consistent revenue streams and building business track record");
        }
        
        // Credit behavior recommendations
        if (financialData.numberOfLatePayments() > 2) {
            recommendations.add("Improve payment discipline - history of late payments increases risk profile");
        }
        
        // Historical trend recommendations
        if (historicalAnalysis != null) {
            if ("Deteriorating".equals(historicalAnalysis.trend())) {
                recommendations.add("Risk trend is deteriorating - immediate action needed to reverse negative trajectory");
            }
            if (historicalAnalysis.deteriorationCount() > 2) {
                recommendations.add("Multiple risk deteriorations detected - consider comprehensive financial review");
            }
        }
        
        // Positive reinforcement
        if (recommendations.isEmpty()) {
            recommendations.add("Maintain current financial discipline and continue monitoring key metrics");
            recommendations.add("Consider gradual business expansion while maintaining healthy financial ratios");
        }
        
        return recommendations;
    }
    
    /**
     * Calculate risk trend direction
     */
    private String calculateRiskTrend(List<CreditRiskAssessment> assessments) {
        if (assessments.size() < 2) {
            return "Insufficient Data";
        }
        
        // Compare most recent vs. previous assessments
        double recentAvg = assessments.stream()
                .limit(3)
                .mapToDouble(CreditRiskAssessment::getRiskScore)
                .average()
                .orElse(0.0);
        
        double olderAvg = assessments.stream()
                .skip(3)
                .limit(3)
                .mapToDouble(CreditRiskAssessment::getRiskScore)
                .average()
                .orElse(recentAvg);
        
        if (recentAvg > olderAvg + 10) {
            return "Deteriorating";
        } else if (recentAvg < olderAvg - 10) {
            return "Improving";
        } else {
            return "Stable";
        }
    }
    
    /**
     * Count number of risk deteriorations
     */
    private int countDeteriorations(List<CreditRiskAssessment> assessments) {
        int count = 0;
        for (int i = 0; i < assessments.size() - 1; i++) {
            if (assessments.get(i).getRiskScore() > assessments.get(i + 1).getRiskScore() + 5) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Calculate average risk score
     */
    private double calculateAverageRiskScore(List<CreditRiskAssessment> assessments) {
        return assessments.stream()
                .mapToDouble(CreditRiskAssessment::getRiskScore)
                .average()
                .orElse(0.0);
    }
    
    /**
     * Calculate average transaction amount
     */
    private BigDecimal calculateAverageTransactionAmount(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(transactions.size()), 2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate total transaction volume
     */
    private BigDecimal calculateTotalVolume(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Count large transactions (> 10k)
     */
    private int countLargeTransactions(List<Transaction> transactions) {
        return (int) transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0)
                .count();
    }
    
    /**
     * Detect irregular transaction patterns
     */
    private boolean detectIrregularPatterns(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return false;
        }
        
        // Check for sudden spikes in transaction amounts
        BigDecimal avgAmount = calculateAverageTransactionAmount(transactions);
        BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(5));
        
        return transactions.stream()
                .anyMatch(t -> t.getAmount().compareTo(threshold) > 0);
    }
    
    /**
     * Record for historical analysis results
     */
    public record HistoricalAnalysis(
            int assessmentCount,
            String trend,
            int deteriorationCount,
            double averageRiskScore
    ) {
    }
    
    /**
     * Record for transaction pattern analysis
     */
    public record TransactionPatternAnalysis(
            int transactionCount,
            BigDecimal averageAmount,
            BigDecimal totalVolume,
            int largeTransactionCount,
            boolean irregularPatternsDetected
    ) {
    }
}
