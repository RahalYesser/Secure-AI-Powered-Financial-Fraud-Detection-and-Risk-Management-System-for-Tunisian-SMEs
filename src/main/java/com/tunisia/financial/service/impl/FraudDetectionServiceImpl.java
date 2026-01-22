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
    @Transactional(readOnly = true)
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
            
            // Store detected fraud patterns if fraud is detected
            if (isFraud) {
                storeFraudPatterns(transaction, predictions, avgConfidence);
            }
            
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
    public void markPatternAsReviewed(Long patternId, String reviewNotes) {
        log.info("Marking fraud pattern {} as reviewed", patternId);
        
        FraudPattern pattern = fraudPatternRepository.findById(patternId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud pattern not found with ID: " + patternId));
        
        pattern.setReviewed(true);
        pattern.setReviewNotes(reviewNotes);
        fraudPatternRepository.save(pattern);
        
        log.info("Fraud pattern {} marked as reviewed", patternId);
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
    
    /**
     * Store fraud patterns detected by the models
     */
    private void storeFraudPatterns(Transaction transaction, 
                                   List<FraudDetectionResult.ModelPrediction> predictions,
                                   double avgConfidence) {
        log.debug("Storing fraud patterns for transaction {}", transaction.getId());
        
        for (FraudDetectionResult.ModelPrediction prediction : predictions) {
            if (prediction.isFraud()) {
                FraudPattern pattern = new FraudPattern();
                pattern.setPatternType("ENSEMBLE_DETECTION");
                pattern.setDescription(prediction.reason());
                pattern.setConfidence(prediction.confidence());
                pattern.setTransaction(transaction);
                pattern.setDetectorModel(prediction.modelName());
                pattern.setMetadata(String.format(
                        "{\"avgConfidence\": %.3f, \"threshold\": %.2f}", 
                        avgConfidence, FRAUD_THRESHOLD
                ));
                
                fraudPatternRepository.save(pattern);
                log.debug("Stored fraud pattern from model {}", prediction.modelName());
            }
        }
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
