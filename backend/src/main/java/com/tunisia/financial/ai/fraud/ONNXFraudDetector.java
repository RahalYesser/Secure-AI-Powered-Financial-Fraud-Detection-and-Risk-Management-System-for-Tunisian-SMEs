package com.tunisia.financial.ai.fraud;

import ai.onnxruntime.*;
import com.tunisia.financial.dto.response.FraudDetectionResult;
import com.tunisia.financial.entity.Transaction;
import com.tunisia.financial.exception.InferenceException;
import com.tunisia.financial.exception.ModelLoadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;

/**
 * Fraud detector implementation using ONNX Runtime
 * Uses trained Random Forest model exported from scikit-learn
 * Features: 16 engineered features matching the Python training pipeline
 */
@Component
@Slf4j
public class ONNXFraudDetector implements FraudDetectionStrategy {
    
    private OrtEnvironment env;
    private OrtSession session;
    private static final String MODEL_NAME = "ONNX-Runtime";
    private static final int NUM_FEATURES = 16;
    
    // Scaler parameters from training (scaler_params.json)
    private static final float[] FEATURE_MEANS = {
        176.11806945657798f,   // amount
        4.485907065437638f,    // amount_log
        10.756585071221405f,   // amount_sqrt
        14.262125f,            // hour
        -0.1642743416323041f,  // hour_sin
        -0.22607262462447378f, // hour_cos
        0.182375f,             // is_night
        0.495625f,             // is_business_hours
        1.50625f,              // transaction_type_encoded
        1.996875f,             // merchant_category_encoded
        2.60225f,              // country_encoded
        0.18432503329052466f,  // device_risk_score
        0.18491228265239915f,  // ip_risk_score
        0.05761423580342194f,  // risk_score_product
        0.18461865797146132f,  // risk_score_avg
        0.23464857936364863f   // risk_score_max
    };
    
    private static final float[] FEATURE_STDS = {
        524.2408833882585f,    // amount
        1.0401265049590158f,   // amount_log
        7.772640932280045f,    // amount_sqrt
        5.359166491570787f,    // hour
        0.7080606742737414f,   // hour_sin
        0.6485022672464053f,   // hour_cos
        0.38615328481706496f,  // is_night
        0.49998085900862227f,  // is_business_hours
        1.1192457002374552f,   // transaction_type_encoded
        1.411245986486785f,    // merchant_category_encoded
        1.8521055416741465f,   // country_encoded
        0.1765113706078477f,   // device_risk_score
        0.1756771873258524f,   // ip_risk_score
        0.1568866064081273f,   // risk_score_product
        0.16513576565660096f,  // risk_score_avg
        0.16833055228504434f   // risk_score_max
    };
    
    @PostConstruct
    public void loadModel() {
        try {
            log.info("Loading ONNX fraud detection model...");
            
            env = OrtEnvironment.getEnvironment();
            
            // Load ONNX model from resources
            ClassPathResource resource = new ClassPathResource("models/fraud_detection.onnx");
            String modelPath = resource.getFile().getAbsolutePath();
            
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            session = env.createSession(modelPath, opts);
            
            log.info("✓ ONNX fraud detection model loaded successfully");
            log.info("  Model inputs: {}", session.getInputNames());
            log.info("  Model outputs: {}", session.getOutputNames());
        } catch (Exception e) {
            log.error("Failed to load ONNX model - using fallback detection", e);
            session = null; // Graceful degradation
        }
    }
    
    
    @Override
    public FraudDetectionResult.ModelPrediction detect(Transaction transaction) {
        try {
            log.debug("Running ONNX fraud detection for transaction {}", transaction.getId());
            
            // Extract and normalize features
            float[] features = extractAndNormalizeFeatures(transaction);
            
            double confidence;
            if (session != null) {
                // Use ONNX model
                confidence = performONNXInference(features);
            } else {
                // Fallback to rule-based
                confidence = performRuleBasedInference(transaction);
            }
            
            boolean isFraud = confidence > 0.5;
            String reason = determineReason(transaction, confidence, features);
            
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
    
    /**
     * Extract and normalize 16 features matching the training pipeline
     */
    private float[] extractAndNormalizeFeatures(Transaction transaction) {
        float[] raw = new float[NUM_FEATURES];
        
        // 0-2: Amount features
        float amount = transaction.getAmount().floatValue();
        raw[0] = amount;
        raw[1] = (float) Math.log1p(amount);
        raw[2] = (float) Math.sqrt(amount);
        
        // 3-7: Time features
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        raw[3] = hour;
        raw[4] = (float) Math.sin(2 * Math.PI * hour / 24);
        raw[5] = (float) Math.cos(2 * Math.PI * hour / 24);
        raw[6] = (hour >= 0 && hour <= 6 || hour >= 22) ? 1.0f : 0.0f; // is_night
        raw[7] = (hour >= 9 && hour <= 17) ? 1.0f : 0.0f; // is_business_hours
        
        // 8-10: Categorical encoded features
        raw[8] = encodeTransactionType(transaction);
        raw[9] = encodeMerchantCategory(transaction);
        raw[10] = encodeCountry(transaction);
        
        // 11-15: Risk scores
        float deviceRisk = calculateDeviceRiskScore(transaction);
        float ipRisk = calculateIpRiskScore(transaction);
        raw[11] = deviceRisk;
        raw[12] = ipRisk;
        raw[13] = deviceRisk * ipRisk; // risk_score_product
        raw[14] = (deviceRisk + ipRisk) / 2; // risk_score_avg
        raw[15] = Math.max(deviceRisk, ipRisk); // risk_score_max
        
        // Normalize using training scaler parameters
        float[] normalized = new float[NUM_FEATURES];
        for (int i = 0; i < NUM_FEATURES; i++) {
            normalized[i] = (raw[i] - FEATURE_MEANS[i]) / FEATURE_STDS[i];
        }
        
        return normalized;
    }
    
    private float encodeTransactionType(Transaction transaction) {
        // Match Python encoding: ['ATM', 'Online', 'POS', 'QR']
        return switch (transaction.getType()) {
            case WITHDRAWAL -> 0.0f;  // ATM
            case PAYMENT -> 1.0f;     // Online
            case TRANSFER -> 2.0f;    // POS
            case DEPOSIT -> 3.0f;     // QR
        };
    }
    
    private float encodeMerchantCategory(Transaction transaction) {
        // Match Python encoding: ['Clothing', 'Electronics', 'Food', 'Grocery', 'Travel']
        // Map transaction types to merchant categories
        return switch (transaction.getType()) {
            case PAYMENT -> 2.0f;      // Food
            case TRANSFER -> 3.0f;     // Grocery
            case WITHDRAWAL -> 4.0f;   // Travel (ATM)
            case DEPOSIT -> 3.0f;      // Grocery
        };
    }
    
    private float encodeCountry(Transaction transaction) {
        // Match Python encoding: ['DE', 'FR', 'NG', 'TR', 'UK', 'US']
        // Default to a common country code (e.g., US = 5)
        return 5.0f;
    }
    
    private float calculateDeviceRiskScore(Transaction transaction) {
        float score = 0.0f;
        
        // High amount increases risk
        float amount = transaction.getAmount().floatValue();
        if (amount > 5000) score += 0.3f;
        else if (amount > 2000) score += 0.15f;
        
        // Night hours increase risk
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        if (hour >= 0 && hour <= 6 || hour >= 22) score += 0.25f;
        
        // Use existing fraud score if available
        if (transaction.getFraudScore() != null) {
            score = transaction.getFraudScore().floatValue();
        }
        
        return Math.min(score, 1.0f);
    }
    
    private float calculateIpRiskScore(Transaction transaction) {
        float score = 0.0f;
        
        // Withdrawal transactions have higher IP risk
        if (transaction.getType().name().contains("WITHDRAWAL")) {
            score += 0.2f;
        }
        
        // High amount transactions
        if (transaction.getAmount().compareTo(BigDecimal.valueOf(3000)) > 0) {
            score += 0.15f;
        }
        
        // Weekend transactions
        int dayOfWeek = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().getValue();
        if (dayOfWeek >= 6) score += 0.1f;
        
        return Math.min(score, 1.0f);
    }
    
    private double performONNXInference(float[] features) {
        try {
            // Create input tensor
            long[] shape = {1, NUM_FEATURES};
            FloatBuffer buffer = FloatBuffer.wrap(features);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);
            
            // Run inference
            OrtSession.Result results = session.run(Collections.singletonMap("float_input", inputTensor));
            
            // Extract prediction  
            // RandomForest models return: [label, probability_distribution]
            // Probability output is index 1
            Object probabilityOutput = results.get(1).getValue();
            
            log.debug("ONNX output class: {}", probabilityOutput.getClass().getName());
            log.debug("ONNX output value: {}", probabilityOutput);
            
            double fraudProbability = 0.5; // Default fallback
            
            // The output is a List containing Maps {class_id -> probability}
            if (probabilityOutput instanceof List) {
                List<?> resultList = (List<?>) probabilityOutput;
                if (!resultList.isEmpty()) {
                    // Get first prediction (we only have 1 sample)
                    Object mapObject = resultList.get(0);
                    log.debug("Map object class: {}", mapObject.getClass().getName());
                    
                    // OnnxMap extends AbstractMap, so it should work with Map interface methods
                    // But direct casting fails, so let's try to access values via iteration
                    if (mapObject instanceof java.util.Map) {
                        java.util.Map<?, ?> probMap = (java.util.Map<?, ?>) mapObject;
                        Object class1Prob = probMap.get(1L); // Class 1 = fraud
                        if (class1Prob != null) {
                            fraudProbability = ((Number) class1Prob).doubleValue();
                            log.info("ONNX fraud probability: {}", fraudProbability);
                        }
                    } else {
                        log.warn("Cannot cast to Map, using fallback. Type: {}", mapObject.getClass().getName());
                    }
                }
            } else {
                log.warn("Unexpected output type: {}", probabilityOutput.getClass().getName());
            }
            
            inputTensor.close();
            results.close();
            
            return fraudProbability;
        } catch (Exception e) {
            log.error("ONNX inference failed", e);
            return 0.5; // Neutral fallback
        }
    }
    
    private double performRuleBasedInference(Transaction transaction) {
        if (transaction == null) {
            log.warn("Transaction is null, returning neutral score");
            return 0.5;
        }
        
        double baseScore = 0.25;
        
        if (transaction.getAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            baseScore += 0.3;
        }
        
        int hour = transaction.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getHour();
        if (hour < 6 || hour > 22) {
            baseScore += 0.25;
        }
        
        return Math.min(baseScore, 1.0);
    }
    
    private String determineReason(Transaction transaction, double confidence, float[] features) {
        if (confidence < 0.3) {
            return "Transaction within normal parameters";
        }
        
        StringBuilder reason = new StringBuilder();
        
        // Denormalize features for interpretation
        float amount = features[0] * FEATURE_STDS[0] + FEATURE_MEANS[0];
        float hour = features[3] * FEATURE_STDS[3] + FEATURE_MEANS[3];
        float deviceRisk = features[11] * FEATURE_STDS[11] + FEATURE_MEANS[11];
        float ipRisk = features[12] * FEATURE_STDS[12] + FEATURE_MEANS[12];
        
        if (amount > 5000) {
            reason.append("High transaction amount (").append(String.format("%.2f", amount)).append("). ");
        }
        
        if (hour < 6 || hour > 22) {
            reason.append("Unusual transaction time (").append(String.format("%.0fh", hour)).append("). ");
        }
        
        if (deviceRisk > 0.6 || ipRisk > 0.6) {
            reason.append("High risk score detected. ");
        }
        
        if (reason.length() == 0) {
            reason.append("Multiple fraud indicators detected by AI model");
        }
        
        return reason.toString().trim();
    }
}
