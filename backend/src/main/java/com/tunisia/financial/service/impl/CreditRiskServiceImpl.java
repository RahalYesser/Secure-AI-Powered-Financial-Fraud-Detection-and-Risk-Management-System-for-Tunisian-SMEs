package com.tunisia.financial.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tunisia.financial.ai.risk.DJLRiskModel;
import com.tunisia.financial.ai.risk.HistoricalRiskAnalyzer;
import com.tunisia.financial.ai.risk.ONNXRiskModel;
import com.tunisia.financial.ai.risk.TensorFlowRiskModel;
import com.tunisia.financial.dto.FinancialData;
import com.tunisia.financial.dto.response.RiskAssessment;
import com.tunisia.financial.dto.response.RiskReport;
import com.tunisia.financial.entity.CreditRiskAssessment;
import com.tunisia.financial.entity.User;
import com.tunisia.financial.enumerations.RiskCategory;
import com.tunisia.financial.exception.UserNotFoundException;
import com.tunisia.financial.repository.CreditRiskRepository;
import com.tunisia.financial.repository.UserRepository;
import com.tunisia.financial.service.CreditRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of CreditRiskService using ensemble of AI models
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreditRiskServiceImpl implements CreditRiskService {
    
    private final DJLRiskModel djlRiskModel;
    private final ONNXRiskModel onnxRiskModel;
    private final TensorFlowRiskModel tensorFlowRiskModel;
    private final HistoricalRiskAnalyzer historicalAnalyzer;
    private final CreditRiskRepository creditRiskRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    private static final String ENSEMBLE_METHOD = "Weighted Average";
    
    @Override
    @Transactional
    public RiskAssessment assessCreditRisk(FinancialData financialData) {
        try {
            log.info("Performing credit risk assessment for SME user {}", financialData.smeUserId());
            
            // Validate user exists
            User smeUser = userRepository.findById(financialData.smeUserId())
                    .orElseThrow(() -> new UserNotFoundException("SME user not found with ID: " + financialData.smeUserId()));
            
            // Ensemble approach: combine multiple models
            var djlPrediction = djlRiskModel.assess(financialData);
            var onnxPrediction = onnxRiskModel.assess(financialData);
            var tfPrediction = tensorFlowRiskModel.assess(financialData);
            
            List<RiskAssessment.ModelRiskPrediction> predictions = Arrays.asList(
                    djlPrediction, onnxPrediction, tfPrediction
            );
            
            // Calculate ensemble risk score (weighted average)
            double ensembleScore = calculateEnsembleScore(predictions);
            RiskCategory ensembleCategory = RiskCategory.fromScore(ensembleScore);
            
            String summary = generateAssessmentSummary(ensembleScore, ensembleCategory, predictions);
            
            // Persist the assessment
            CreditRiskAssessment assessment = persistAssessment(
                    financialData, smeUser, ensembleScore, ensembleCategory, 
                    summary, predictions
            );
            
            log.info("Credit risk assessment completed for user {} with score {} and category {}", 
                    financialData.smeUserId(), ensembleScore, ensembleCategory);
            
            return new RiskAssessment(
                    assessment.getId(),
                    financialData.smeUserId(),
                    ensembleScore,
                    ensembleCategory,
                    summary,
                    predictions,
                    Instant.now()
            );
        } catch (Exception e) {
            log.error("Credit risk assessment failed for user {}", financialData.smeUserId(), e);
            throw new RuntimeException("Credit risk assessment failed", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public RiskReport generateRiskReport(Long assessmentId) {
        log.info("Generating risk report for assessment {}", assessmentId);
        
        CreditRiskAssessment assessment = creditRiskRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with ID: " + assessmentId));
        
        User smeUser = assessment.getSmeUser();
        
        // Get historical analysis
        HistoricalRiskAnalyzer.HistoricalAnalysis historicalAnalysis = 
                historicalAnalyzer.analyzeRiskTrend(smeUser);
        
        // Build financial metrics
        RiskReport.FinancialMetrics financialMetrics = new RiskReport.FinancialMetrics(
                assessment.getDebtRatio(),
                BigDecimal.ZERO, // Would be calculated from actual data
                BigDecimal.ZERO,
                BigDecimal.valueOf(assessment.getRiskScore()),
                determineLiquidityStatus(assessment)
        );
        
        // Build historical analysis record
        RiskReport.HistoricalAnalysis historicalRecord = new RiskReport.HistoricalAnalysis(
                historicalAnalysis.assessmentCount(),
                historicalAnalysis.trend(),
                historicalAnalysis.trend(),
                historicalAnalysis.deteriorationCount(),
                0.0 // Would be calculated from transaction data
        );
        
        // Build market conditions
        RiskReport.MarketConditions marketConditions = new RiskReport.MarketConditions(
                assessment.getIndustrySector(),
                "Stable", // Would be fetched from market data service
                calculateSectorRiskScore(assessment.getIndustrySector()),
                "Current economic indicators suggest stable growth"
        );
        
        // Extract key risk factors
        List<RiskReport.RiskFactor> keyRiskFactors = extractRiskFactors(assessment);
        
        // Generate recommendations using financial data
        FinancialData reconstructedData = reconstructFinancialData(assessment);
        List<String> recommendations = historicalAnalyzer.generateRecommendations(
                reconstructedData, historicalAnalysis
        );
        
        // Generate executive summary
        String executiveSummary = generateExecutiveSummary(assessment, keyRiskFactors);
        
        return new RiskReport(
                assessment.getId(),
                assessment.getId(),
                smeUser.getId(),
                smeUser.getEmail(), // Using email as business name for now
                assessment.getRiskCategory(),
                assessment.getRiskScore(),
                financialMetrics,
                historicalRecord,
                marketConditions,
                keyRiskFactors,
                recommendations,
                executiveSummary,
                Instant.now()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public CreditRiskAssessment getAssessmentById(Long assessmentId) {
        return creditRiskRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with ID: " + assessmentId));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CreditRiskAssessment> getAssessmentsByUserId(UUID userId) {
        return creditRiskRepository.findBySmeUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getAllAssessments(Pageable pageable) {
        return creditRiskRepository.findAll(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getAssessmentsByCategory(RiskCategory category, Pageable pageable) {
        return creditRiskRepository.findByRiskCategory(category, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getHighRiskAssessments(Pageable pageable) {
        return creditRiskRepository.findHighRiskAssessments(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CreditRiskAssessment> getAssessmentsAboveThreshold(Double threshold) {
        return creditRiskRepository.findByRiskScoreGreaterThanEqual(threshold);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CreditRiskAssessment getMostRecentAssessment(UUID userId) {
        return creditRiskRepository.findFirstBySmeUserIdOrderByAssessedAtDesc(userId)
                .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CreditRiskAssessment> getAssessmentsByDateRange(Instant startDate, Instant endDate) {
        return creditRiskRepository.findByAssessedAtBetween(startDate, endDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getUnreviewedAssessments(Pageable pageable) {
        return creditRiskRepository.findByReviewedFalse(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getUnreviewedHighRiskAssessments(Pageable pageable) {
        return creditRiskRepository.findUnreviewedHighRiskAssessments(pageable);
    }
    
    @Override
    @Transactional
    public void markAssessmentAsReviewed(Long assessmentId, String reviewNotes) {
        CreditRiskAssessment assessment = getAssessmentById(assessmentId);
        assessment.setReviewed(true);
        assessment.setReviewNotes(reviewNotes);
        creditRiskRepository.save(assessment);
        log.info("Assessment {} marked as reviewed", assessmentId);
    }
    
    @Override
    public void updateModel(String modelType) {
        log.info("Model update requested for: {}", modelType);
        // In production, this would reload the model from disk/cloud
        log.info("Model {} update completed", modelType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<RiskCategory, Long> getRiskStatistics() {
        List<Object[]> results = creditRiskRepository.countByRiskCategory();
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (RiskCategory) row[0],
                        row -> (Long) row[1]
                ));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateAverageRiskScore(UUID userId) {
        return creditRiskRepository.calculateAverageRiskScoreByUserId(userId)
                .orElse(0.0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<CreditRiskAssessment> getAssessmentsBySector(String sector, Pageable pageable) {
        return creditRiskRepository.findByIndustrySector(sector, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Double calculateAverageRiskScoreBySector(String sector) {
        return creditRiskRepository.calculateAverageRiskScoreBySector(sector)
                .orElse(0.0);
    }
    
    // Private helper methods
    
    private double calculateEnsembleScore(List<RiskAssessment.ModelRiskPrediction> predictions) {
        // Weighted average: DJL=35%, ONNX=35%, TensorFlow=30%
        double djlScore = predictions.get(0).riskScore() * 0.35;
        double onnxScore = predictions.get(1).riskScore() * 0.35;
        double tfScore = predictions.get(2).riskScore() * 0.30;
        
        return djlScore + onnxScore + tfScore;
    }
    
    private String generateAssessmentSummary(double score, RiskCategory category, 
                                             List<RiskAssessment.ModelRiskPrediction> predictions) {
        StringBuilder summary = new StringBuilder();
        summary.append("Ensemble Risk Score: ").append(String.format("%.2f", score))
                .append(" (").append(category).append("). ");
        
        long criticalModels = predictions.stream()
                .filter(p -> p.predictedCategory() == RiskCategory.CRITICAL)
                .count();
        
        if (criticalModels > 1) {
            summary.append("Multiple models indicate CRITICAL risk level. ");
        }
        
        return summary.toString();
    }
    
    private CreditRiskAssessment persistAssessment(FinancialData financialData, User smeUser,
                                                   double score, RiskCategory category,
                                                   String summary, List<RiskAssessment.ModelRiskPrediction> predictions) {
        CreditRiskAssessment assessment = new CreditRiskAssessment();
        assessment.setSmeUser(smeUser);
        assessment.setRiskScore(score);
        assessment.setRiskCategory(category);
        assessment.setAssessmentSummary(summary);
        assessment.setAnnualRevenue(financialData.annualRevenue());
        assessment.setTotalAssets(financialData.totalAssets());
        assessment.setTotalLiabilities(financialData.totalLiabilities());
        assessment.setDebtRatio(BigDecimal.valueOf(financialData.calculateDebtRatio()));
        assessment.setIndustrySector(financialData.industrySector());
        assessment.setYearsInBusiness(financialData.yearsInBusiness());
        assessment.setCreditHistoryScore(financialData.creditHistoryScore());
        assessment.setEnsembleMethod(ENSEMBLE_METHOD);
        
        try {
            assessment.setFinancialData(objectMapper.writeValueAsString(financialData));
            assessment.setModelPredictions(objectMapper.writeValueAsString(predictions));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize data as JSON", e);
        }
        
        return creditRiskRepository.save(assessment);
    }
    
    private String determineLiquidityStatus(CreditRiskAssessment assessment) {
        if (assessment.getDebtRatio() != null) {
            double debtRatio = assessment.getDebtRatio().doubleValue();
            if (debtRatio > 0.7) return "Poor";
            if (debtRatio > 0.5) return "Fair";
            return "Good";
        }
        return "Unknown";
    }
    
    private double calculateSectorRiskScore(String sector) {
        // Simplified sector risk scoring
        if (sector == null) return 50.0;
        String lowerSector = sector.toLowerCase();
        if (lowerSector.contains("technology")) return 35.0;
        if (lowerSector.contains("healthcare")) return 40.0;
        if (lowerSector.contains("retail")) return 60.0;
        if (lowerSector.contains("hospitality")) return 70.0;
        return 50.0;
    }
    
    private List<RiskReport.RiskFactor> extractRiskFactors(CreditRiskAssessment assessment) {
        List<RiskReport.RiskFactor> factors = new ArrayList<>();
        
        // Debt ratio factor
        if (assessment.getDebtRatio() != null) {
            double debtRatio = assessment.getDebtRatio().doubleValue();
            if (debtRatio > 0.6) {
                factors.add(new RiskReport.RiskFactor(
                        "High Debt Ratio",
                        debtRatio > 0.8 ? RiskCategory.CRITICAL : RiskCategory.HIGH,
                        debtRatio * 100,
                        "Debt-to-asset ratio is " + String.format("%.1f%%", debtRatio * 100)
                ));
            }
        }
        
        // Credit history factor
        if (assessment.getCreditHistoryScore() != null && assessment.getCreditHistoryScore() < 50) {
            factors.add(new RiskReport.RiskFactor(
                    "Poor Credit History",
                    assessment.getCreditHistoryScore() < 30 ? RiskCategory.CRITICAL : RiskCategory.HIGH,
                    (100 - assessment.getCreditHistoryScore()),
                    "Credit history score is low at " + assessment.getCreditHistoryScore()
            ));
        }
        
        // Business maturity factor
        if (assessment.getYearsInBusiness() != null && assessment.getYearsInBusiness() < 2) {
            factors.add(new RiskReport.RiskFactor(
                    "Limited Business History",
                    RiskCategory.MEDIUM,
                    50.0,
                    "Business established less than 2 years ago"
            ));
        }
        
        return factors;
    }
    
    private FinancialData reconstructFinancialData(CreditRiskAssessment assessment) {
        // Reconstruct FinancialData from assessment for recommendations
        return new FinancialData(
                assessment.getSmeUser().getId(),
                assessment.getAnnualRevenue() != null ? assessment.getAnnualRevenue() : BigDecimal.ZERO,
                assessment.getTotalAssets() != null ? assessment.getTotalAssets() : BigDecimal.ZERO,
                assessment.getTotalLiabilities() != null ? assessment.getTotalLiabilities() : BigDecimal.ZERO,
                BigDecimal.ZERO, // monthlyCashFlow not stored
                BigDecimal.ZERO, // outstandingDebt not stored
                0, // numberOfEmployees not stored
                assessment.getYearsInBusiness() != null ? assessment.getYearsInBusiness() : 0,
                assessment.getIndustrySector() != null ? assessment.getIndustrySector() : "Unknown",
                assessment.getCreditHistoryScore() != null ? assessment.getCreditHistoryScore() : 50.0,
                0, // numberOfLatePayments not stored
                null, null, null, // ratios not stored
                assessment.getAssessedAt()
        );
    }
    
    private String generateExecutiveSummary(CreditRiskAssessment assessment, 
                                           List<RiskReport.RiskFactor> riskFactors) {
        StringBuilder summary = new StringBuilder();
        summary.append("Credit risk assessment for SME user ").append(assessment.getSmeUser().getId())
                .append(" indicates ").append(assessment.getRiskCategory()).append(" risk level ");
        summary.append("with an overall risk score of ").append(String.format("%.2f", assessment.getRiskScore()))
                .append(". ");
        
        if (!riskFactors.isEmpty()) {
            summary.append("Key concerns include: ");
            summary.append(riskFactors.stream()
                    .map(RiskReport.RiskFactor::factorName)
                    .collect(Collectors.joining(", ")));
            summary.append(". ");
        }
        
        if (assessment.getRiskCategory() == RiskCategory.LOW || assessment.getRiskCategory() == RiskCategory.MEDIUM) {
            summary.append("Overall financial position is acceptable for credit consideration.");
        } else {
            summary.append("Careful evaluation and risk mitigation strategies are recommended.");
        }
        
        return summary.toString();
    }
}
