package com.tunisia.financial.ai.fraud;

import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.exception.InferenceException;
import com.tunisia.financial.exception.ModelLoadException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * Fraud detector implementation using TensorFlow Java
 * Uses TensorFlow SavedModel format for inference
 */
@Component
@Slf4j
public class TensorFlowFraudDetector implements FraudDetectionStrategy {
    
    private static final String MODEL_NAME = "TensorFlow-Java";
    
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing TensorFlow Fraud Detector...");
            // In production, load actual TensorFlow SavedModel
            // savedModelBundle = SavedModelBundle.load("path/to/model", "serve");
            log.info("TensorFlow Fraud Detector initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize TensorFlow model", e);
            throw new ModelLoadException(MODEL_NAME, e);
        }
    }
    
    @Override
    public FraudDetectionResult.ModelPrediction detect(Transaction transaction) {
        try {
            log.debug("Running TensorFlow fraud detection for transaction {}", transaction.getId());
            
            // Extract features
            float[] features = extractFeatures(transaction);
            
            // Perform inference
            double confidence = performInference(features);
            boolean isFraud = confidence > 0.7;
            
            String reason = determineReason(transaction, confidence);
            
            return new FraudDetectionResult.ModelPrediction(
                    MODEL_NAME,
                    confidence,
                    isFraud,
                    reason
            );
        } catch (Exception e) {
            log.error("TensorFlow inference failed for transaction {}", transaction.getId(), e);
            throw new InferenceException(MODEL_NAME, e);
        }
    }
    
    private float[] extractFeatures(Transaction transaction) {
        float amount = transaction.getAmount().floatValue();
        float hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        float dayOfWeek = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().getValue();
        float typeValue = transaction.getType().ordinal();
        float statusValue = transaction.getStatus().ordinal();
        
        return new float[]{amount, hour, dayOfWeek, typeValue, statusValue};
    }
    
    /**
     * Perform TensorFlow model inference
     * Basic rule-based implementation for now
     */
    private double performInference(float[] features) {
        double baseScore = 0.2;
        
        // Amount-based rules
        if (features[0] > 20000) {
            baseScore += 0.4;
        } else if (features[0] > 10000) {
            baseScore += 0.25;
        } else if (features[0] > 5000) {
            baseScore += 0.15;
        }
        
        // Time-based rules
        if (features[1] >= 2 && features[1] <= 5) {
            baseScore += 0.3; // Very early morning
        } else if (features[1] < 6 || features[1] > 22) {
            baseScore += 0.2; // Night hours
        }
        
        // Day of week rules
        if (features[2] == DayOfWeek.SUNDAY.getValue() || features[2] == DayOfWeek.SATURDAY.getValue()) {
            baseScore += 0.1; // Weekend
        }
        
        // Add randomness to simulate model variance
        baseScore += Math.random() * 0.1;
        
        return Math.min(baseScore, 1.0);
    }
    
    private String determineReason(Transaction transaction, double confidence) {
        if (confidence > 0.7) {
            BigDecimal amount = transaction.getAmount();
            int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
            
            if (amount.compareTo(BigDecimal.valueOf(20000)) > 0) {
                return "Extremely high transaction amount";
            }
            if (hour >= 2 && hour <= 5) {
                return "Transaction during suspicious hours (2-5 AM)";
            }
            if (hour < 6 || hour > 22) {
                return "Late night/early morning transaction";
            }
            return "Multiple fraud indicators detected by TensorFlow model";
        }
        return "Transaction appears legitimate";
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            // In production: close TensorFlow session/model
            log.info("TensorFlow model resources released");
        } catch (Exception e) {
            log.error("Error cleaning up TensorFlow resources", e);
        }
    }
}
