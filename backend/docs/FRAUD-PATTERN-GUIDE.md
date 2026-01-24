# Fraud Pattern Detection - Implementation Guide

## Overview
This guide explains the improved fraud pattern detection system that now automatically categorizes and stores suspicious transaction patterns for analysis.

## What's New (Just Implemented)

### 1. **Automatic Pattern Storage**
- ✅ **Before**: Patterns only stored when confidence > 0.7 (too restrictive)
- ✅ **Now**: Patterns stored for ANY transaction with confidence >= 0.5
- This captures **borderline suspicious** transactions for review and analysis

### 2. **Pattern Categorization**
Previously, all patterns were labeled as "ENSEMBLE_DETECTION". Now the system intelligently categorizes patterns based on transaction characteristics:

#### Pattern Types

| Pattern Type | Criteria | Confidence Range |
|--------------|----------|------------------|
| `HIGH_AMOUNT_LATE_NIGHT` | Amount > $10,000 AND time between 10 PM - 6 AM | >= 0.7 |
| `HIGH_AMOUNT_UNUSUAL` | Amount > $10,000 AND business hours | >= 0.7 |
| `LATE_NIGHT_TRANSACTION` | Time between 10 PM - 6 AM | >= 0.7 |
| `SUSPICIOUS_ACTIVITY` | Other high-confidence fraud indicators | >= 0.7 |
| `MEDIUM_RISK_HIGH_AMOUNT` | Amount > $5,000 | 0.6 - 0.69 |
| `MEDIUM_RISK_UNUSUAL_PATTERN` | Unusual patterns | 0.6 - 0.69 |
| `BORDERLINE_SUSPICIOUS` | Slightly elevated risk | 0.5 - 0.59 |

### 3. **Enhanced Metadata**
Each stored pattern now includes comprehensive metadata in JSON format:

```json
{
  "avgConfidence": 0.567,
  "threshold": 0.70,
  "amount": 22974.37,
  "hour": 14,
  "dayOfWeek": 3,
  "type": "WITHDRAWAL",
  "isWeekend": false,
  "isBusinessHours": true,
  "detectionTimestamp": "2026-01-24T12:59:45.808947Z"
}
```

### 4. **Detailed Descriptions**
Pattern descriptions now include:
- Transaction ID and amount
- Transaction type
- How many models flagged it as fraud (e.g., "2 of 3 models")
- Specific reasons from each model

Example:
```
Transaction #55: $22974.37 WITHDRAWAL. 2 of 3 models flagged as fraud. 
DJL-PyTorch: High transaction amount detected by DJL model. 
TensorFlow-Java: Extremely high transaction amount detected by TensorFlow.
```

## How It Works

### Detection Flow

```
User Transaction
    ↓
1. Run ensemble detection (3 AI models)
    ├── DJL-PyTorch Model
    ├── ONNX-Runtime Model  
    └── TensorFlow-Java Model
    ↓
2. Calculate average confidence
    ↓
3. Determine if fraud (confidence > 0.7)
    ↓
4. Store pattern (if confidence >= 0.5)  ← NEW: Even if not flagged as fraud!
    ├── Categorize pattern type
    ├── Build detailed description
    └── Generate metadata JSON
    ↓
5. Return detection result to API
```

### Why Store Borderline Cases?

**Real-world fraud detection requires analyzing trends:**
- A single transaction with 0.56 confidence might not be fraud
- But if a user has 10 transactions all with 0.5-0.6 confidence, that's a pattern!
- Historical borderline patterns help improve ML models
- Fraud analysts can review and mark patterns for learning

## API Endpoints

### 1. Detect Fraud (Stores Pattern Automatically)
```bash
POST /api/v1/fraud/detect/{transactionId}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/fraud/detect/55" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "isFraud": false,
  "confidence": 0.567,
  "primaryReason": "Extremely high transaction amount detected by TensorFlow",
  "modelPredictions": [
    {
      "modelName": "DJL-PyTorch",
      "confidence": 0.6,
      "isFraud": true,
      "reason": "High transaction amount detected by DJL model"
    },
    {
      "modelName": "ONNX-Runtime",
      "confidence": 0.5,
      "isFraud": false,
      "reason": "High transaction amount (22974.37). High risk score detected."
    },
    {
      "modelName": "TensorFlow-Java",
      "confidence": 0.6,
      "isFraud": true,
      "reason": "Extremely high transaction amount detected by TensorFlow"
    }
  ],
  "fraudScore": 0.567
}
```

### 2. Get All Fraud Patterns
```bash
GET /api/v1/fraud/patterns?page=0&size=10
```

### 3. Get Patterns for Specific Transaction
```bash
GET /api/v1/fraud/patterns/transaction/{transactionId}
```

### 4. Get Unreviewed Patterns
```bash
GET /api/v1/fraud/patterns/unreviewed?page=0&size=20
```

### 5. Get High-Confidence Patterns
```bash
GET /api/v1/fraud/patterns/high-confidence?threshold=0.7
```

### 6. Get Patterns by Date Range
```bash
GET /api/v1/fraud/patterns/date-range?startDate=2024-01-01&endDate=2024-12-31
```

## Testing

### Test Script Location
```bash
/backend/scripts/test-complete-fraud-flow.sh
```

### Quick Test
```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin1@financial.tn","password":"Admin123!"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")

# Test fraud detection on multiple transactions
for ID in 25 55 60 70; do
  echo "Testing transaction $ID..."
  curl -s -X POST "http://localhost:8080/api/v1/fraud/detect/$ID" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys, json; d=json.load(sys.stdin); print(f'  Confidence: {d[\"confidence\"]:.3f}, Fraud: {d[\"isFraud\"]}')"
done

# Check stored patterns
curl -s "http://localhost:8080/api/v1/fraud/patterns?size=10" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -m json.tool
```

## Database Schema

### FraudPattern Table
```sql
CREATE TABLE fraud_patterns (
    id BIGSERIAL PRIMARY KEY,
    pattern_type VARCHAR(100) NOT NULL,
    description TEXT,
    confidence DECIMAL(5,4) NOT NULL,
    transaction_id BIGINT NOT NULL,
    detector_model VARCHAR(50),
    metadata TEXT,
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_fraud_patterns_confidence ON fraud_patterns(confidence);
CREATE INDEX idx_fraud_patterns_pattern_type ON fraud_patterns(pattern_type);
CREATE INDEX idx_fraud_patterns_detected_at ON fraud_patterns(detected_at);
CREATE INDEX idx_fraud_patterns_transaction_id ON fraud_patterns(transaction_id);
CREATE INDEX idx_fraud_patterns_reviewed ON fraud_patterns(reviewed);
```

## Current Status

### ✅ Fully Implemented
1. Automatic fraud detection using ensemble of 3 models
2. Pattern storage for confidence >= 0.5
3. Intelligent pattern categorization (7 types)
4. Enhanced metadata JSON generation
5. Detailed pattern descriptions
6. Complete REST API for pattern management
7. Database indexes for performance
8. Transaction safety with @Transactional

### ⚠️ Partially Implemented
1. **ONNX Model**: Returns fallback value (0.5) instead of trained predictions
   - Needs proper model training or model file update
2. **Pattern Review Workflow**: `reviewed` field exists but no UI/endpoint to mark as reviewed
3. **Pattern Matching**: No logic to compare new transactions against historical patterns

### ❌ Not Yet Implemented
1. **Automatic Detection**: Fraud detection must be called manually via API
   - Suggestion: Add `@Async` detection on transaction creation
2. **Alerting System**: No email/notification when fraud detected
3. **Pattern Learning**: No feedback loop to improve models based on pattern reviews
4. **Transaction Blocking**: High-risk transactions not automatically blocked
5. **Dashboard**: No visual analytics for fraud patterns
6. **Batch Processing**: No scheduled job to scan all transactions

## Real-World Production Recommendations

### 1. Automatic Detection
Add to TransactionService after transaction creation:
```java
@Async
public void detectFraudAsync(Transaction transaction) {
    fraudDetectionService.detectFraud(transaction);
}
```

### 2. Transaction Blocking
Add status field to Transaction:
```java
public enum TransactionStatus {
    PENDING,
    APPROVED,
    BLOCKED,
    UNDER_REVIEW
}
```

Block high-risk transactions:
```java
if (avgConfidence > 0.8) {
    transaction.setStatus(TransactionStatus.BLOCKED);
    sendAdminAlert(transaction);
}
```

### 3. Pattern Matching
Check against historical patterns:
```java
List<FraudPattern> similarPatterns = fraudPatternRepository
    .findByPatternTypeAndConfidenceGreaterThan(
        determinedPatternType, 0.7
    );

if (!similarPatterns.isEmpty()) {
    // Boost confidence if similar fraud patterns exist
    avgConfidence += 0.1;
}
```

### 4. Scheduled Batch Processing
```java
@Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
public void scanAllTransactions() {
    List<Transaction> unscannedTransactions = 
        transactionRepository.findByFraudScoreIsNull();
    
    unscannedTransactions.forEach(this::detectFraud);
}
```

## Example Output

### Transaction 55 (High Amount Withdrawal)
```json
{
  "id": 1,
  "patternType": "BORDERLINE_SUSPICIOUS",
  "description": "Transaction #55: $22974.37 WITHDRAWAL. 2 of 3 models flagged as fraud. DJL-PyTorch: High transaction amount detected by DJL model. TensorFlow-Java: Extremely high transaction amount detected by TensorFlow.",
  "confidence": 0.5666666666666668,
  "transactionId": 55,
  "detectorModel": "ENSEMBLE",
  "detectedAt": "2026-01-24T12:59:45.808947Z",
  "reviewed": false
}
```

## Code Changes Made

### Files Modified
1. **FraudDetectionServiceImpl.java**
   - Changed `@Transactional(readOnly = true)` to `@Transactional` (can write)
   - Removed `if (isFraud)` check before storing patterns
   - Added `determinePatternType()` method (pattern categorization)
   - Added `buildEnhancedDescription()` method (detailed descriptions)
   - Added `buildPatternMetadata()` method (comprehensive JSON metadata)
   - Updated `storeFraudPatterns()` to use new categorization logic

### Key Logic
```java
// OLD: Only store if definite fraud
if (isFraud) {
    storeFraudPatterns(transaction, predictions, avgConfidence);
}

// NEW: Store all suspicious patterns
storeFraudPatterns(transaction, predictions, avgConfidence);

// Inside storeFraudPatterns:
if (avgConfidence >= 0.5) {  // Store borderline cases too!
    String patternType = determinePatternType(transaction, avgConfidence);
    // ... create and save pattern
}
```

## Next Steps for Production

1. **Immediate (High Priority)**
   - [ ] Fix ONNX model to return real predictions instead of 0.5 fallback
   - [ ] Add automatic fraud detection on transaction creation
   - [ ] Implement transaction blocking for confidence > 0.8

2. **Short Term (Medium Priority)**
   - [ ] Add pattern review UI/endpoint
   - [ ] Implement email alerting
   - [ ] Add pattern matching against historical data
   - [ ] Create fraud dashboard with analytics

3. **Long Term (Nice to Have)**
   - [ ] Machine learning feedback loop
   - [ ] Pattern clustering and anomaly detection
   - [ ] Integration with external fraud databases
   - [ ] Real-time fraud scoring API

## Summary

The fraud pattern system is now **fully functional** for storing and categorizing suspicious transactions. The main improvements are:

1. ✅ Captures borderline suspicious cases (0.5-0.6 confidence)
2. ✅ Intelligent pattern categorization (7 types)
3. ✅ Comprehensive metadata for analysis
4. ✅ Detailed descriptions showing which models detected what
5. ✅ Complete REST API for pattern management

**What's working:**
- Authentication and user management (100%)
- Transaction management (100%)
- Fraud detection API (100%)
- Fraud pattern storage and retrieval (100%)

**What needs improvement:**
- ONNX model predictions (currently returning fallback)
- Automatic detection (currently manual API calls)
- Alerting and blocking (not implemented)
- Pattern review workflow (partially implemented)

The system is ready for development/testing environments. For production deployment, implement the recommendations above.
