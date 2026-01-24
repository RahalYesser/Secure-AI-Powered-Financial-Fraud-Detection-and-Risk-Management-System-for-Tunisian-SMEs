package com.tunisia.financial.service.impl;

import com.tunisia.financial.ai.fraud.DJLFraudDetector;
import com.tunisia.financial.ai.fraud.ONNXFraudDetector;
import com.tunisia.financial.ai.fraud.TensorFlowFraudDetector;
import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.dto.response.FraudPatternResponse;
import com.tunisia.financial.entity.FraudPattern;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.exception.FraudDetectionException;
import com.tunisia.financial.repository.FraudPatternRepository;
import com.tunisia.financial.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of FraudDetectionService using ensemble of AI models
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FraudDetectionServiceImpl implements FraudDetectionService {
    
    private final DJLFraudDetector djlDetector;
    private final ONNXFraudDetector onnxDetector;
    private final TensorFlowFraudDetector tfDetector;
    private final FraudPatternRepository fraudPatternRepository;
    
    private static final double FRAUD_THRESHOLD = 0.7;
    
    @Override
    @Transactional
    public FraudDetectionResult detectFraud(Transaction transaction) {
        try {
            log.info("Running fraud detection for transaction {}", transaction.getId());
            
            // Ensemble approach: combine multiple models
            var djlResult = djlDetector.detect(transaction);
            var onnxResult = onnxDetector.detect(transaction);
            var tfResult = tfDetector.detect(transaction);
            
            List<FraudDetectionResult.ModelPrediction> predictions = 
                    List.of(djlResult, onnxResult, tfResult);
            
            // Weighted average of confidence scores
            double avgConfidence = (djlResult.confidence() + 
                                   onnxResult.confidence() + 
                                   tfResult.confidence()) / 3.0;
            
            boolean isFraud = avgConfidence > FRAUD_THRESHOLD;
            
            // Determine primary reason from the model with highest confidence
            String primaryReason = predictions.stream()
                    .max(Comparator.comparingDouble(FraudDetectionResult.ModelPrediction::confidence))
                    .map(FraudDetectionResult.ModelPrediction::reason)
                    .orElse("Multiple indicators detected");
            
            FraudDetectionResult result = new FraudDetectionResult(
                    isFraud,
                    avgConfidence,
                    primaryReason,
                    predictions,
                    avgConfidence
            );
            
            // Store detected fraud patterns - now includes borderline cases (>= 0.5 confidence)
            // This allows tracking of suspicious patterns even if not flagged as definite fraud
            storeFraudPatterns(transaction, predictions, avgConfidence);
            
            log.info("Fraud detection completed for transaction {}. Fraud: {}, Confidence: {}", 
                    transaction.getId(), isFraud, avgConfidence);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error detecting fraud for transaction {}", transaction.getId(), e);
            throw new FraudDetectionException("Failed to detect fraud", e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FraudPattern> getFraudPatterns() {
        log.debug("Fetching all fraud patterns");
        return fraudPatternRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<FraudPatternResponse> getFraudPatterns(Pageable pageable) {
        log.debug("Fetching fraud patterns with pagination");
        return fraudPatternRepository.findAll(pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FraudPatternResponse> getFraudPatternsByTransactionId(Long transactionId) {
        log.debug("Fetching fraud patterns for transaction {}", transactionId);
        return fraudPatternRepository.findByTransactionId(transactionId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<FraudPatternResponse> getUnreviewedFraudPatterns(Pageable pageable) {
        log.debug("Fetching unreviewed fraud patterns");
        return fraudPatternRepository.findByReviewedFalse(pageable)
                .map(this::convertToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FraudPatternResponse> getHighConfidenceFraudPatterns(Double threshold) {
        log.debug("Fetching high confidence fraud patterns with threshold {}", threshold);
        return fraudPatternRepository.findByConfidenceGreaterThanEqual(threshold)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateModel(String modelType) {
        log.info("Model update requested for type: {}", modelType);
        
        // In production, this would reload/retrain the specified model
        switch (modelType.toUpperCase()) {
            case "DJL":
                log.info("Updating DJL model...");
                // djlDetector.reload();
                break;
            case "ONNX":
                log.info("Updating ONNX model...");
                // onnxDetector.reload();
                break;
            case "TENSORFLOW":
                log.info("Updating TensorFlow model...");
                // tfDetector.reload();
                break;
            default:
                log.warn("Unknown model type: {}", modelType);
                throw new IllegalArgumentException("Unknown model type: " + modelType);
        }
        
        log.info("Model {} updated successfully", modelType);
    }
    
    @Override
    public void markPatternAsReviewed(Long patternId, String reviewNotes, java.util.UUID reviewerId) {
        log.info("Marking fraud pattern {} as reviewed by user {}", patternId, reviewerId);
        
        FraudPattern pattern = fraudPatternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud pattern not found with ID: " + patternId));
        
        pattern.setReviewed(true);
        pattern.setReviewNotes(reviewNotes);
        pattern.setReviewedBy(reviewerId);
        pattern.setReviewedAt(Instant.now());
        fraudPatternRepository.save(pattern);
        
        log.info("Fraud pattern {} marked as reviewed by user {}", patternId, reviewerId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FraudPatternResponse> getFraudPatternsByDateRange(Instant startDate, Instant endDate) {
        log.debug("Fetching fraud patterns between {} and {}", startDate, endDate);
        return fraudPatternRepository.findByDetectedAtBetween(startDate, endDate)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    
    @Override
    @Transactional(readOnly = true)
    public com.tunisia.financial.dto.fraud.FraudStatistics getFraudStatistics() {
        log.debug("Fetching fraud statistics");
        
        List<FraudPattern> allPatterns = fraudPatternRepository.findAll();
        
        // Count by resolution status
        long total = allPatterns.size();
        long resolved = allPatterns.stream().filter(FraudPattern::getReviewed).count();
        long unresolved = total - resolved;
        
        // Group by severity (derived from confidence)
        java.util.Map<String, Long> bySeverity = allPatterns.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        pattern -> {
                            double conf = pattern.getConfidence();
                            if (conf >= 0.9) return "CRITICAL";
                            if (conf >= 0.75) return "HIGH";
                            if (conf >= 0.6) return "MEDIUM";
                            return "LOW";
                        },
                        java.util.stream.Collectors.counting()
                ));
        
        // Group by pattern type
        java.util.Map<String, Long> byType = allPatterns.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FraudPattern::getPatternType,
                        java.util.stream.Collectors.counting()
                ));
        
        // Patterns over time (last 30 days)
        Instant thirtyDaysAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        java.util.Map<String, Long> dailyCounts = allPatterns.stream()
                .filter(p -> p.getDetectedAt().isAfter(thirtyDaysAgo))
                .collect(java.util.stream.Collectors.groupingBy(
                        pattern -> pattern.getDetectedAt()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                                .toString(),
                        java.util.stream.Collectors.counting()
                ));
        
        // Convert to time series list
        List<com.tunisia.financial.dto.fraud.FraudStatistics.PatternTimeSeries> timeSeries =
                dailyCounts.entrySet().stream()
                        .map(entry -> com.tunisia.financial.dto.fraud.FraudStatistics.PatternTimeSeries.builder()
                                .date(entry.getKey())
                                .count(entry.getValue())
                                .build())
                        .sorted(Comparator.comparing(com.tunisia.financial.dto.fraud.FraudStatistics.PatternTimeSeries::getDate))
                        .collect(Collectors.toList());
        
        return com.tunisia.financial.dto.fraud.FraudStatistics.builder()
                .totalPatterns(total)
                .resolvedPatterns(resolved)
                .unresolvedPatterns(unresolved)
                .patternsBySeverity(bySeverity)
                .patternsByType(byType)
                .patternsOverTime(timeSeries)
                .build();
    }
    
    /**
     * Store fraud patterns detected by the models
     * Enhanced to categorize patterns and store borderline cases
     */
    private void storeFraudPatterns(Transaction transaction, 
                                   List<FraudDetectionResult.ModelPrediction> predictions,
                                   double avgConfidence) {
        log.debug("Storing fraud patterns for transaction {} with confidence {}", 
                transaction.getId(), avgConfidence);
        
        // Store patterns for high-confidence fraud (>0.7) or borderline cases (0.5-0.7)
        if (avgConfidence >= 0.5) {
            String patternType = determinePatternType(transaction, avgConfidence);
            String enhancedDescription = buildEnhancedDescription(transaction, predictions);
            String metadata = buildPatternMetadata(transaction, avgConfidence);
            
            // Store one consolidated pattern per transaction
            FraudPattern pattern = new FraudPattern();
            pattern.setPatternType(patternType);
            pattern.setDescription(enhancedDescription);
            pattern.setConfidence(avgConfidence);
            pattern.setTransaction(transaction);
            pattern.setDetectorModel("ENSEMBLE");
            pattern.setMetadata(metadata);
            pattern.setDetectedAt(Instant.now());
            pattern.setReviewed(false);
            
            fraudPatternRepository.save(pattern);
            log.info("Stored {} pattern for transaction {} with confidence {}", 
                    patternType, transaction.getId(), avgConfidence);
        }
    }
    
    /**
     * Determine the type of fraud pattern based on transaction characteristics
     */
    private String determinePatternType(Transaction transaction, double confidence) {
        BigDecimal amount = transaction.getAmount();
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        
        // High confidence patterns
        if (confidence >= 0.7) {
            if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
                return hour >= 22 || hour <= 6 ? 
                    "HIGH_AMOUNT_LATE_NIGHT" : "HIGH_AMOUNT_UNUSUAL";
            } else if (hour >= 22 || hour <= 6) {
                return "LATE_NIGHT_TRANSACTION";
            } else {
                return "SUSPICIOUS_ACTIVITY";
            }
        }
        
        // Medium confidence patterns (borderline)
        if (confidence >= 0.6) {
            if (amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
                return "MEDIUM_RISK_HIGH_AMOUNT";
            } else {
                return "MEDIUM_RISK_UNUSUAL_PATTERN";
            }
        }
        
        // Low-medium confidence
        return "BORDERLINE_SUSPICIOUS";
    }
    
    /**
     * Build enhanced description with details from all models
     */
    private String buildEnhancedDescription(Transaction transaction, 
                                           List<FraudDetectionResult.ModelPrediction> predictions) {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Transaction #%d: $%.2f %s. ", 
                transaction.getId(), 
                transaction.getAmount(), 
                transaction.getType()));
        
        long fraudCount = predictions.stream().filter(FraudDetectionResult.ModelPrediction::isFraud).count();
        desc.append(String.format("%d of %d models flagged as fraud. ", fraudCount, predictions.size()));
        
        // Add reasons from models that detected fraud
        predictions.stream()
                .filter(FraudDetectionResult.ModelPrediction::isFraud)
                .forEach(p -> desc.append(String.format("%s: %s. ", p.modelName(), p.reason())));
        
        return desc.toString().trim();
    }
    
    /**
     * Build detailed metadata JSON for pattern analysis
     */
    private String buildPatternMetadata(Transaction transaction, double avgConfidence) {
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        int dayOfWeek = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                .getDayOfWeek().getValue();
        
        return String.format(
                "{\"avgConfidence\": %.3f, \"threshold\": %.2f, \"amount\": %.2f, " +
                "\"hour\": %d, \"dayOfWeek\": %d, \"type\": \"%s\", \"isWeekend\": %b, " +
                "\"isBusinessHours\": %b, \"detectionTimestamp\": \"%s\"}",
                avgConfidence, 
                FRAUD_THRESHOLD, 
                transaction.getAmount(),
                hour,
                dayOfWeek,
                transaction.getType(),
                dayOfWeek >= 6,
                hour >= 9 && hour <= 17,
                Instant.now()
        );
    }
    
    /**
     * Convert FraudPattern entity to response DTO
     */
    private FraudPatternResponse convertToResponse(FraudPattern pattern) {
        return new FraudPatternResponse(
                pattern.getId(),
                pattern.getPatternType(),
                pattern.getDescription(),
                pattern.getConfidence(),
                pattern.getTransaction() != null ? pattern.getTransaction().getId() : null,
                pattern.getDetectorModel(),
                pattern.getDetectedAt(),
                pattern.getReviewed()
        );
    }
}
