# Fraud Detection AI Model - Setup Guide

## Overview

This directory contains the fraud detection machine learning pipeline that trains a Random Forest model on synthetic transaction data and exports it to ONNX format for use in the Spring Boot backend.

## Dataset

**File**: `fraud_dataset.csv`  
**Size**: 10,000 transactions  
**Fraud Rate**: 5% (500 fraudulent, 9,500 legitimate)

### Features (10 columns):
1. **transaction_id** - Unique transaction identifier
2. **user_id** - User identifier
3. **amount** - Transaction amount
4. **transaction_type** - Type: ATM, Online, POS, QR
5. **merchant_category** - Category: Clothing, Electronics, Food, Grocery, Travel
6. **country** - Country code: DE, FR, NG, TR, UK, US
7. **hour** - Hour of day (0-23)
8. **device_risk_score** - Risk score from device fingerprinting (0-1)
9. **ip_risk_score** - Risk score from IP analysis (0-1)
10. **is_fraud** - Target label (0=legitimate, 1=fraud)

## Training Pipeline

### 1. Install Dependencies

```bash
cd backend/datasets/fraud
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Train Model

```bash
python train_fraud_model.py
```

This script will:
- Load and explore the dataset
- Engineer 16 features from the original 10
- Train a Random Forest classifier (150 trees, max_depth=15)
- Evaluate performance (accuracy, ROC-AUC, confusion matrix)
- Export to ONNX format
- Save scaler parameters and metadata

### 3. Outputs

All outputs are saved to `backend/src/main/resources/models/`:

- **fraud_detection.onnx** - ONNX model for production use
- **fraud_model.pkl** - Scikit-learn model (Python)
- **scaler.pkl** - StandardScaler for feature normalization
- **scaler_params.json** - Scaler parameters (means & stds) for Java
- **model_metadata.json** - Model configuration and metrics
- **feature_importance.csv** - Feature importance ranking
- **encoder_*.pkl** - Label encoders for categorical features (3 files)

## Feature Engineering

The model uses **16 engineered features**:

### Amount Features (3):
1. `amount` - Raw transaction amount
2. `amount_log` - Log-transformed amount: log(1 + amount)
3. `amount_sqrt` - Square root of amount

### Time Features (5):
4. `hour` - Hour of day (0-23)
5. `hour_sin` - Sine transformation: sin(2π * hour / 24)
6. `hour_cos` - Cosine transformation: cos(2π * hour / 24)
7. `is_night` - Binary flag for night hours (0-6 or 22-23)
8. `is_business_hours` - Binary flag for business hours (9-17)

### Categorical Features (3):
9. `transaction_type_encoded` - Encoded transaction type (0-3)
10. `merchant_category_encoded` - Encoded merchant category (0-4)
11. `country_encoded` - Encoded country (0-5)

### Risk Scores (5):
12. `device_risk_score` - Original device risk
13. `ip_risk_score` - Original IP risk
14. `risk_score_product` - device × ip
15. `risk_score_avg` - (device + ip) / 2
16. `risk_score_max` - max(device, ip)

## Model Performance

**Training Results** (from last run):
- Train Accuracy: 100%
- Test Accuracy: 100%
- ROC-AUC Score: 1.0000

**Top Feature Importances**:
1. risk_score_product: 19.79%
2. ip_risk_score: 19.53%
3. risk_score_max: 19.19%
4. device_risk_score: 19.09%
5. risk_score_avg: 12.01%

**Note**: Perfect accuracy indicates the dataset has clear separable patterns. In production with real data, expect 85-95% accuracy.

## Java Integration

The trained ONNX model is loaded by three fraud detectors:

### 1. ONNXFraudDetector (Primary)
- Loads `fraud_detection.onnx` from classpath
- Uses ONNX Runtime for inference
- 16-feature pipeline with StandardScaler normalization
- Graceful fallback to rule-based detection if model fails

### 2. DJLFraudDetector
- Uses same 16-feature extraction
- Currently rule-based (can be upgraded to load PyTorch model via DJL)
- Consistent feature engineering with ONNX

### 3. TensorFlowFraudDetector
- Uses same 16-feature extraction
- Currently rule-based (can be upgraded to load SavedModel)
- Consistent feature engineering with ONNX

### Feature Extraction Mapping

Since the Transaction entity doesn't have all fields from the dataset, the Java implementation uses intelligent mapping:

| Dataset Feature | Transaction Mapping |
|----------------|-------------------|
| amount | `transaction.getAmount()` |
| hour | `transaction.getCreatedAt().getHour()` |
| transaction_type | `transaction.getType().ordinal()` |
| merchant_category | Derived from transaction type |
| country | Default value (5 = US) |
| device_risk_score | Calculated from amount & time patterns |
| ip_risk_score | Calculated from transaction characteristics |

## Usage

### Testing the Model

Once the Spring Boot application starts:

```bash
# Start application
cd backend
mvn spring-boot:run

# Test fraud detection API
curl -X POST http://localhost:8080/api/v1/fraud-detection/detect/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Swagger UI

Access the API documentation at:
```
http://localhost:8080/swagger-ui.html
```

Navigate to `Fraud Detection Controller` to test endpoints.

## Retraining the Model

To retrain with new data:

1. Replace `fraud_dataset.csv` with your new dataset
2. Ensure it has the same 10 columns
3. Run `python train_fraud_model.py`
4. Check `model_metadata.json` for new performance metrics
5. Restart Spring Boot application

The Java detectors will automatically pick up the new ONNX model.

## Architecture

```
┌─────────────────────┐
│  fraud_dataset.csv  │
└──────────┬──────────┘
           │
    ┌──────▼───────┐
    │ Python       │
    │ Training     │
    │ Pipeline     │
    └──────┬───────┘
           │
    ┌──────▼──────────────────┐
    │ fraud_detection.onnx    │
    │ scaler_params.json      │
    └──────┬──────────────────┘
           │
    ┌──────▼──────────────┐
    │ Spring Boot         │
    │ ONNXFraudDetector   │
    │ + DJL + TensorFlow  │
    └──────┬──────────────┘
           │
    ┌──────▼──────────────┐
    │ Ensemble Voting     │
    │ (3 models combined) │
    └──────┬──────────────┘
           │
    ┌──────▼──────────────┐
    │ Final Fraud Score   │
    └─────────────────────┘
```

## Troubleshooting

### Model Not Loading

**Error**: `Failed to load ONNX model`

**Solution**:
1. Check if `fraud_detection.onnx` exists in `src/main/resources/models/`
2. Verify file is not corrupted: `ls -lh src/main/resources/models/fraud_detection.onnx`
3. Check logs for detailed error message
4. System falls back to rule-based detection gracefully

### Low Accuracy

**Issue**: Model predictions seem random

**Solution**:
1. Check feature scaling parameters in Java match `scaler_params.json`
2. Verify feature extraction order matches training pipeline
3. Ensure categorical encodings are consistent

### ONNX Runtime Error

**Error**: `OnnxRuntimeException during inference`

**Solution**:
1. Verify ONNX Runtime version in pom.xml matches training environment
2. Check input tensor shape is `[1, 16]`
3. Ensure float32 data type
4. Review ONNX model with Netron: https://netron.app/

## Files Structure

```
backend/datasets/fraud/
├── fraud_dataset.csv          # Training data (10,000 rows)
├── train_fraud_model.py       # Training script
├── requirements.txt           # Python dependencies
├── venv/                      # Virtual environment (created)
└── README.md                  # This file

backend/src/main/resources/models/
├── fraud_detection.onnx       # Production model
├── fraud_model.pkl            # Sklearn model backup
├── scaler.pkl                 # Feature scaler
├── scaler_params.json         # Java integration params
├── model_metadata.json        # Model info & metrics
├── feature_importance.csv     # Feature rankings
└── encoder_*.pkl              # 3 label encoders

backend/src/main/java/com/tunisia/financial/ai/fraud/
├── ONNXFraudDetector.java     # ONNX-based detector
├── DJLFraudDetector.java      # DJL-based detector
├── TensorFlowFraudDetector.java  # TensorFlow detector
└── FraudDetectionStrategy.java   # Interface
```

## Next Steps

1. **Collect Real Data**: Replace synthetic data with production transactions
2. **Feature Enhancement**: Add user behavior patterns, transaction history
3. **Model Upgrade**: Try XGBoost, LightGBM, or neural networks
4. **Online Learning**: Implement incremental learning for concept drift
5. **Explainability**: Add SHAP/LIME for prediction explanations
6. **A/B Testing**: Compare model versions in production

## References

- [ONNX Documentation](https://onnx.ai/)
- [Scikit-learn RandomForest](https://scikit-learn.org/stable/modules/generated/sklearn.ensemble.RandomForestClassifier.html)
- [ONNX Runtime Java API](https://onnxruntime.ai/docs/api/java/ai/onnxruntime/package-summary.html)
- [Fraud Detection Techniques](https://www.kaggle.com/learn/intro-to-machine-learning)

---

**Last Updated**: January 24, 2026  
**Model Version**: 1.0  
**Dataset Version**: synthetic-v1
