package com.tunisia.financial.seeder;

import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.RiskCategory;
import com.tunisia.financial.enumerations.UserRole;
import com.tunisia.financial.repository.CreditRiskRepository;
import com.tunisia.financial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Credit Risk Assessment seeder for creating sample risk assessments
 * Runs after user seeder to create assessments for SME users
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class CreditRiskSeeder implements CommandLineRunner {

    private final CreditRiskRepository creditRiskRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        try {
            // Check if credit_risk_assessments table is empty
            if (creditRiskRepository.count() == 0) {
                log.info("Credit risk assessments table is empty. Starting risk assessment seeding...");
                seedCreditRiskAssessments();
                log.info("Credit risk assessment seeding completed successfully!");
            } else {
                log.info("Credit risk assessments table already contains data. Skipping risk assessment seeding.");
            }
        } catch (Exception e) {
            log.warn("Credit risk assessment seeding skipped: {}", e.getMessage());
        }
    }

    private void seedCreditRiskAssessments() {
        // Get all SME users
        List<User> smeUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.SME_USER)
                .toList();

        if (smeUsers.isEmpty()) {
            log.warn("No SME users found. Skipping credit risk assessment seeding.");
            return;
        }

        List<CreditRiskAssessment> assessments = new ArrayList<>();

        // Create 2-3 assessments per SME user (historical assessments)
        for (User smeUser : smeUsers) {
            int numAssessments = 2 + random.nextInt(2); // 2-3 assessments
            
            for (int i = 0; i < numAssessments; i++) {
                CreditRiskAssessment assessment = createCreditRiskAssessment(smeUser, i);
                assessments.add(assessment);
            }
        }

        // Save all assessments
        creditRiskRepository.saveAll(assessments);

        // Count by category
        long lowRisk = assessments.stream().filter(a -> a.getRiskCategory() == RiskCategory.LOW).count();
        long mediumRisk = assessments.stream().filter(a -> a.getRiskCategory() == RiskCategory.MEDIUM).count();
        long highRisk = assessments.stream().filter(a -> a.getRiskCategory() == RiskCategory.HIGH).count();
        long criticalRisk = assessments.stream().filter(a -> a.getRiskCategory() == RiskCategory.CRITICAL).count();

        log.info("Created {} credit risk assessments:", assessments.size());
        log.info("  - {} LOW risk assessments", lowRisk);
        log.info("  - {} MEDIUM risk assessments", mediumRisk);
        log.info("  - {} HIGH risk assessments", highRisk);
        log.info("  - {} CRITICAL risk assessments", criticalRisk);
        log.info("  - Assessments for {} SME users", smeUsers.size());
    }

    private CreditRiskAssessment createCreditRiskAssessment(User smeUser, int assessmentIndex) {
        CreditRiskAssessment assessment = new CreditRiskAssessment();
        
        // Set SME user
        assessment.setSmeUser(smeUser);
        
        // Generate realistic financial data
        BigDecimal annualRevenue = generateAnnualRevenue();
        BigDecimal totalAssets = generateTotalAssets(annualRevenue);
        BigDecimal totalLiabilities = generateTotalLiabilities(totalAssets);
        BigDecimal debtRatio = totalLiabilities.divide(totalAssets, 4, RoundingMode.HALF_UP);
        
        assessment.setAnnualRevenue(annualRevenue);
        assessment.setTotalAssets(totalAssets);
        assessment.setTotalLiabilities(totalLiabilities);
        assessment.setDebtRatio(debtRatio);
        
        // Set industry sector
        assessment.setIndustrySector(getRandomIndustrySector());
        
        // Set years in business (more years for older assessments)
        int baseYears = 1 + random.nextInt(10);
        assessment.setYearsInBusiness(baseYears + assessmentIndex);
        
        // Generate credit history score
        assessment.setCreditHistoryScore(40.0 + random.nextDouble() * 55.0); // 40-95
        
        // Calculate risk score based on financial metrics
        double riskScore = calculateRiskScore(
                debtRatio.doubleValue(),
                assessment.getCreditHistoryScore(),
                assessment.getYearsInBusiness(),
                annualRevenue.doubleValue()
        );
        assessment.setRiskScore(riskScore);
        
        // Determine risk category
        RiskCategory category = determineRiskCategory(riskScore);
        assessment.setRiskCategory(category);
        
        // Generate assessment summary
        assessment.setAssessmentSummary(generateAssessmentSummary(assessment));
        
        // Generate financial data JSON
        assessment.setFinancialData(generateFinancialDataJson(assessment));
        
        // Generate model predictions JSON
        assessment.setModelPredictions(generateModelPredictions(riskScore));
        
        // Set ensemble method
        String[] ensembleMethods = {"VOTING", "WEIGHTED_AVERAGE", "STACKING"};
        assessment.setEnsembleMethod(ensembleMethods[random.nextInt(ensembleMethods.length)]);
        
        // Set market conditions
        assessment.setMarketConditions(generateMarketConditions());
        
        // Set assessment time (older assessments for higher index)
        Instant assessedAt = Instant.now().minus(
                (long) (assessmentIndex * 90 + random.nextInt(30)), 
                ChronoUnit.DAYS
        );
        assessment.setAssessedAt(assessedAt);
        assessment.setUpdatedAt(assessedAt);
        
        // Randomly review some assessments (70% reviewed)
        if (random.nextDouble() < 0.7) {
            assessment.setReviewed(true);
            assessment.setReviewNotes(generateReviewNotes(category));
        } else {
            assessment.setReviewed(false);
        }
        
        return assessment;
    }

    private BigDecimal generateAnnualRevenue() {
        // Generate realistic annual revenue between $100K and $50M
        double base = 100000 + random.nextDouble() * 49900000;
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateTotalAssets(BigDecimal annualRevenue) {
        // Assets typically 0.5x to 3x of annual revenue
        double multiplier = 0.5 + random.nextDouble() * 2.5;
        return annualRevenue.multiply(BigDecimal.valueOf(multiplier))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal generateTotalLiabilities(BigDecimal totalAssets) {
        // Liabilities typically 20% to 80% of assets
        double ratio = 0.2 + random.nextDouble() * 0.6;
        return totalAssets.multiply(BigDecimal.valueOf(ratio))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String getRandomIndustrySector() {
        String[] sectors = {
            "Technology", "Manufacturing", "Retail", "Healthcare",
            "Construction", "Food & Beverage", "Transportation",
            "Professional Services", "Wholesale", "Hospitality",
            "Agriculture", "Real Estate", "Education", "Finance"
        };
        return sectors[random.nextInt(sectors.length)];
    }

    private double calculateRiskScore(double debtRatio, double creditScore, int yearsInBusiness, double revenue) {
        // Calculate risk score (0-100, higher is riskier)
        double score = 0.0;
        
        // Debt ratio impact (0-35 points)
        if (debtRatio > 0.7) {
            score += 30 + (debtRatio - 0.7) * 50;
        } else if (debtRatio > 0.5) {
            score += 15 + (debtRatio - 0.5) * 75;
        } else {
            score += debtRatio * 30;
        }
        
        // Credit history impact (0-30 points, inverse relationship)
        score += (100 - creditScore) * 0.3;
        
        // Years in business impact (0-20 points, inverse relationship)
        if (yearsInBusiness < 2) {
            score += 20;
        } else if (yearsInBusiness < 5) {
            score += 15;
        } else if (yearsInBusiness < 10) {
            score += 10;
        } else {
            score += 5;
        }
        
        // Revenue impact (0-15 points)
        if (revenue < 500000) {
            score += 15;
        } else if (revenue < 2000000) {
            score += 10;
        } else if (revenue < 10000000) {
            score += 5;
        }
        
        // Add some randomness (±5 points)
        score += (random.nextDouble() * 10) - 5;
        
        // Ensure score is between 0 and 100
        return Math.max(0, Math.min(100, score));
    }

    private RiskCategory determineRiskCategory(double riskScore) {
        if (riskScore >= 75) {
            return RiskCategory.CRITICAL;
        } else if (riskScore >= 55) {
            return RiskCategory.HIGH;
        } else if (riskScore >= 35) {
            return RiskCategory.MEDIUM;
        } else {
            return RiskCategory.LOW;
        }
    }

    private String generateAssessmentSummary(CreditRiskAssessment assessment) {
        RiskCategory category = assessment.getRiskCategory();
        double riskScore = assessment.getRiskScore();
        
        return switch (category) {
            case LOW -> String.format(
                "Low risk assessment (Score: %.1f/100). Business demonstrates strong financial health with " +
                "debt ratio of %.2f and %d years of operating history. %s sector shows stable performance. " +
                "Credit history score of %.1f indicates reliable payment behavior. Recommended for standard credit terms.",
                riskScore, assessment.getDebtRatio(), assessment.getYearsInBusiness(),
                assessment.getIndustrySector(), assessment.getCreditHistoryScore()
            );
            case MEDIUM -> String.format(
                "Medium risk assessment (Score: %.1f/100). Business shows moderate financial stability with " +
                "debt ratio of %.2f. Operating for %d years in %s sector. Credit history score of %.1f " +
                "requires monitoring. Recommended credit terms with standard safeguards and periodic reviews.",
                riskScore, assessment.getDebtRatio(), assessment.getYearsInBusiness(),
                assessment.getIndustrySector(), assessment.getCreditHistoryScore()
            );
            case HIGH -> String.format(
                "High risk assessment (Score: %.1f/100). Business presents elevated risk factors including " +
                "debt ratio of %.2f and credit history score of %.1f. Operating for %d years in %s sector. " +
                "Recommended stringent credit terms, collateral requirements, and close monitoring of financial performance.",
                riskScore, assessment.getDebtRatio(), assessment.getCreditHistoryScore(),
                assessment.getYearsInBusiness(), assessment.getIndustrySector()
            );
            case CRITICAL -> String.format(
                "Critical risk assessment (Score: %.1f/100). Business shows significant financial distress indicators. " +
                "High debt ratio of %.2f combined with credit history score of %.1f raises serious concerns. " +
                "%d years in %s sector. Recommend declining credit or requiring substantial collateral with very conservative limits.",
                riskScore, assessment.getDebtRatio(), assessment.getCreditHistoryScore(),
                assessment.getYearsInBusiness(), assessment.getIndustrySector()
            );
        };
    }

    private String generateFinancialDataJson(CreditRiskAssessment assessment) {
        return String.format(
            "{\"annualRevenue\": %.2f, \"totalAssets\": %.2f, \"totalLiabilities\": %.2f, " +
            "\"debtRatio\": %.4f, \"currentRatio\": %.2f, \"quickRatio\": %.2f, " +
            "\"profitMargin\": %.2f, \"roe\": %.2f, \"cashFlow\": %.2f}",
            assessment.getAnnualRevenue(),
            assessment.getTotalAssets(),
            assessment.getTotalLiabilities(),
            assessment.getDebtRatio(),
            1.2 + random.nextDouble() * 1.8,  // Current ratio 1.2-3.0
            0.8 + random.nextDouble() * 1.2,  // Quick ratio 0.8-2.0
            0.05 + random.nextDouble() * 0.20, // Profit margin 5-25%
            0.08 + random.nextDouble() * 0.22, // ROE 8-30%
            assessment.getAnnualRevenue().doubleValue() * (0.1 + random.nextDouble() * 0.2) // Cash flow 10-30% of revenue
        );
    }

    private String generateModelPredictions(double riskScore) {
        // Generate predictions from different models
        double djlScore = riskScore + (random.nextDouble() * 10 - 5);
        double onnxScore = riskScore + (random.nextDouble() * 10 - 5);
        double tfScore = riskScore + (random.nextDouble() * 10 - 5);
        
        return String.format(
            "{\"models\": [" +
            "{\"name\": \"DJL_MODEL\", \"score\": %.2f, \"confidence\": %.3f}, " +
            "{\"name\": \"ONNX_MODEL\", \"score\": %.2f, \"confidence\": %.3f}, " +
            "{\"name\": \"TENSORFLOW_MODEL\", \"score\": %.2f, \"confidence\": %.3f}], " +
            "\"ensembleScore\": %.2f, \"ensembleConfidence\": %.3f}",
            Math.max(0, Math.min(100, djlScore)), 0.7 + random.nextDouble() * 0.25,
            Math.max(0, Math.min(100, onnxScore)), 0.7 + random.nextDouble() * 0.25,
            Math.max(0, Math.min(100, tfScore)), 0.7 + random.nextDouble() * 0.25,
            riskScore, 0.75 + random.nextDouble() * 0.20
        );
    }

    private String generateMarketConditions() {
        String[] conditions = {
            "{\"economicGrowth\": \"moderate\", \"interestRates\": \"stable\", \"industryOutlook\": \"positive\"}",
            "{\"economicGrowth\": \"strong\", \"interestRates\": \"rising\", \"industryOutlook\": \"stable\"}",
            "{\"economicGrowth\": \"slow\", \"interestRates\": \"stable\", \"industryOutlook\": \"cautious\"}",
            "{\"economicGrowth\": \"moderate\", \"interestRates\": \"declining\", \"industryOutlook\": \"optimistic\"}",
            "{\"economicGrowth\": \"strong\", \"interestRates\": \"stable\", \"industryOutlook\": \"very_positive\"}"
        };
        return conditions[random.nextInt(conditions.length)];
    }

    private String generateReviewNotes(RiskCategory category) {
        return switch (category) {
            case LOW -> "Assessment reviewed and approved. Business demonstrates strong financial position. " +
                       "Credit facility approved with standard terms. Annual review recommended.";
            case MEDIUM -> "Assessment reviewed. Moderate risk factors identified. Credit approved with " +
                          "enhanced monitoring. Quarterly financial statements required. Review in 6 months.";
            case HIGH -> "Assessment reviewed by senior credit officer. High risk factors require additional " +
                        "safeguards. Credit approved with reduced limits and collateral requirements. Monthly monitoring.";
            case CRITICAL -> "Assessment reviewed by credit committee. Critical risk level identified. " +
                           "Credit application declined pending significant improvement in financial metrics. " +
                           "Recommend re-assessment in 12 months.";
        };
    }
}
