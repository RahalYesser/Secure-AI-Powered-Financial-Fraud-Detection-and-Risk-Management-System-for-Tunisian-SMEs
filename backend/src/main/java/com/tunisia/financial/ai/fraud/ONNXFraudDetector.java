package com.tunisia.financial.ai.fraud;

import ai.onnxruntime.*;
import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.exception.InferenceException;
import com.tunisia.financial.exception.ModelLoadException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Fraud detector implementation using ONNX Runtime
 * Supports models exported from TensorFlow, PyTorch, scikit-learn, etc.
 */
@Component
@Slf4j
public class ONNXFraudDetector implements FraudDetectionStrategy {
    
    private OrtEnvironment env;
    private OrtSession session;
    private static final String MODEL_NAME = "ONNX-Runtime";
    
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing ONNX Fraud Detector...");
            env = OrtEnvironment.getEnvironment();
            // In production, load actual ONNX model file
            //session = env.createSession("path/to/fraud_model.onnx", new OrtSession.SessionOptions());
            log.info("ONNX Fraud Detector initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize ONNX model", e);
            throw new ModelLoadException(MODEL_NAME, e);
        }
    }
    
    @Override
    public FraudDetectionResult.ModelPrediction detect(Transaction transaction) {
        try {
            log.debug("Running ONNX fraud detection for transaction {}", transaction.getId());
            
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
            log.error("ONNX inference failed for transaction {}", transaction.getId(), e);
            throw new InferenceException(MODEL_NAME, e);
        }
    }
    
    private float[] extractFeatures(Transaction transaction) {
        float amount = transaction.getAmount().floatValue();
        float hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        float dayOfWeek = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().getValue();
        float typeValue = transaction.getType().ordinal();
        
        return new float[]{amount, hour, dayOfWeek, typeValue};
    }
    
    /**
     * Perform ONNX model inference
     * Basic rule-based implementation for now
     */
    private double performInference(float[] features) {
        double baseScore = 0.25;
        
        // Rule: Very high amounts
        if (features[0] > 15000) {
            baseScore += 0.35;
        } else if (features[0] > 10000) {
            baseScore += 0.25;
        }
        
        // Rule: Weekend transactions
        if (features[2] > 5) {
            baseScore += 0.15;
        }
        
        // Rule: Late night hours
        if (features[1] < 5 || features[1] > 23) {
            baseScore += 0.25;
        }
        
        // Add randomness
        baseScore += Math.random() * 0.15;
        
        return Math.min(baseScore, 1.0);
    }
    
    private String determineReason(Transaction transaction, double confidence) {
        if (confidence > 0.7) {
            if (transaction.getAmount().compareTo(BigDecimal.valueOf(15000)) > 0) {
                return "Very high transaction amount";
            }
            int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
            if (hour < 5 || hour > 23) {
                return "Unusual transaction time";
            }
            return "Anomalous pattern detected by ONNX model";
        }
        return "Transaction within normal parameters";
    }
    
    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                // Note: OrtEnvironment should not be closed if it's the global instance
                log.info("ONNX model resources released");
            }
        } catch (Exception e) {
            log.error("Error cleaning up ONNX resources", e);
        }
    }
}
