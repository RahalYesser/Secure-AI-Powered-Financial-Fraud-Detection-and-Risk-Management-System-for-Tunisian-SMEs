# Fraud Detection System - Complete Testing Guide with Swagger

## Prerequisites

✅ Backend running on http://localhost:8080  
✅ Database seeded with users and transactions  
✅ ONNX fraud detection model loaded

## Data Seeded Automatically

### Users (30 total):
- **10 ADMIN users**: admin1@financial.tn to admin10@financial.tn (Password: Admin123!)
- **8 FINANCIAL_ANALYST users**: analyst1@financial.tn to analyst8@financial.tn (Password: Analyst123!)
- **7 SME_USER users**: sme1@financial.tn to sme7@financial.tn (Password: Sme123!)
- **5 AUDITOR users**: auditor1@financial.tn to auditor5@financial.tn (Password: Auditor123!)

### Transactions (70 total):
- **IDs 1-50**: Legitimate transactions (normal amounts $10-$2000, business hours)
- **IDs 51-60**: Suspicious transactions (high amounts $15,000-$50,000, odd hours)
- **IDs 61-70**: Mixed transactions for analysts/admins

---

## Step-by-Step Testing Flow

### Step 1: Access Swagger UI

1. Open browser: http://localhost:8080/swagger-ui.html
2. You should see all API endpoints organized by controllers

### Step 2: Authenticate (Get JWT Token)

#### 2.1 Expand "Auth" Controller
Click on **"Auth"** section to see authentication endpoints

#### 2.2 Login as Admin
1. Click on **POST /api/v1/auth/login**
2. Click **"Try it out"**
3. Replace request body with:
```json
{
  "email": "admin1@financial.tn",
  "password": "Admin123!"
}
```
4. Click **"Execute"**
5. Scroll down to **Response body** - you'll see:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "admin1@financial.tn",
  "role": "ADMIN",
  "firstName": "Admin",
  "lastName": "User1"
}
```

#### 2.3 Copy the JWT Token
1. Copy the entire `token` value (starts with `eyJ...`)
2. Scroll to the **top of the page**
3. Click the green **"Authorize"** button (🔓 icon)
4. In the popup, paste your token in the **"Value"** field
5. Click **"Authorize"**, then **"Close"**
6. You're now authenticated! 🎉

---

### Step 3: View Available Transactions

#### 3.1 Get All Transactions
1. Expand **"Transaction Management"** controller
2. Click **GET /api/v1/transactions**
3. Click **"Try it out"**
4. Set parameters:
   - `page`: 0
   - `size`: 20
   - `sortBy`: createdAt
   - `sortDirection`: DESC
5. Click **"Execute"**

**Expected Response:**
```json
{
  "content": [
    {
      "id": 60,
      "amount": 45230.50,
      "type": "WITHDRAWAL",
      "status": "COMPLETED",
      "fraudScore": 0.85,
      "createdAt": "2026-01-24T02:30:00Z",
      ...
    },
    ...
  ],
  "totalElements": 70,
  "totalPages": 4
}
```

Note the **transaction IDs** for testing fraud detection.

---

### Step 4: Test Fraud Detection (Main Feature!)

#### 4.1 Detect Fraud on Legitimate Transaction
1. Expand **"Fraud Detection"** controller
2. Click **POST /api/v1/fraud/detect/{transactionId}**
3. Click **"Try it out"**
4. Enter a **legitimate transaction ID**: `25` (should be low risk)
5. Click **"Execute"**

**Expected Response:**
```json
{
  "isFraud": false,
  "confidence": 0.25,
  "primaryReason": "Transaction within normal parameters",
  "fraudScore": 0.25,
  "modelPredictions": [
    {
      "modelName": "ONNX-Runtime",
      "confidence": 0.18,
      "isFraud": false,
      "reason": "Normal transaction pattern"
    },
    {
      "modelName": "DJL-PyTorch",
      "confidence": 0.30,
      "isFraud": false,
      "reason": "Normal transaction pattern"
    },
    {
      "modelName": "TensorFlow-Java",
      "confidence": 0.28,
      "isFraud": false,
      "reason": "Transaction appears legitimate"
    }
  ]
}
```

✅ **All 3 AI models** agree: **NOT FRAUD** (confidence < 0.5)

#### 4.2 Detect Fraud on Suspicious Transaction
1. Same endpoint: **POST /api/v1/fraud/detect/{transactionId}**
2. Click **"Try it out"**
3. Enter a **suspicious transaction ID**: `55` (high amount, odd hours)
4. Click **"Execute"**

**Expected Response:**
```json
{
  "isFraud": true,
  "confidence": 0.78,
  "primaryReason": "High transaction amount (25450.75). Unusual transaction time (2h).",
  "fraudScore": 0.78,
  "modelPredictions": [
    {
      "modelName": "ONNX-Runtime",
      "confidence": 0.85,
      "isFraud": true,
      "reason": "High transaction amount (25450.75). Unusual transaction time (2h). High risk score detected."
    },
    {
      "modelName": "DJL-PyTorch",
      "confidence": 0.75,
      "isFraud": true,
      "reason": "High transaction amount detected by DJL model"
    },
    {
      "modelName": "TensorFlow-Java",
      "confidence": 0.74,
      "isFraud": true,
      "reason": "Transaction during suspicious hours (2-5 AM)"
    }
  ]
}
```

🚨 **All 3 AI models** agree: **FRAUD DETECTED!** (confidence > 0.5)

---

### Step 5: View Fraud Statistics

#### 5.1 Get Fraud Detection Statistics
1. In **"Fraud Detection"** controller
2. Click **GET /api/v1/fraud/statistics**
3. Click **"Try it out"**
4. Set date range (optional):
   - `startDate`: 2026-01-01T00:00:00Z
   - `endDate`: 2026-01-31T23:59:59Z
5. Click **"Execute"**

**Expected Response:**
```json
{
  "totalTransactions": 70,
  "fraudulentCount": 10,
  "legitimateCount": 60,
  "fraudPercentage": 14.28,
  "averageFraudScore": 0.42,
  "highRiskCount": 8,
  "mediumRiskCount": 12,
  "lowRiskCount": 50
}
```

---

### Step 6: Batch Fraud Detection

#### 6.1 Detect Fraud on Multiple Transactions
1. In **"Fraud Detection"** controller
2. Click **POST /api/v1/fraud/batch-detect**
3. Click **"Try it out"**
4. Enter transaction IDs in request body:
```json
{
  "transactionIds": [25, 55, 56, 30, 58]
}
```
5. Click **"Execute"**

**Expected Response:**
```json
[
  {
    "transactionId": 25,
    "isFraud": false,
    "confidence": 0.23,
    ...
  },
  {
    "transactionId": 55,
    "isFraud": true,
    "confidence": 0.82,
    ...
  },
  ...
]
```

---

### Step 7: View Fraud Patterns

#### 7.1 Get Detected Fraud Patterns
1. In **"Fraud Detection"** controller
2. Click **GET /api/v1/fraud/patterns**
3. Click **"Try it out"**
4. Set pagination:
   - `page`: 0
   - `size`: 20
5. Click **"Execute"**

**Expected Response:**
Shows patterns detected by the system (high-value transactions, unusual times, etc.)

---

### Step 8: Create a New Transaction

#### 8.1 Create Normal Transaction
1. In **"Transaction Management"** controller
2. Click **POST /api/v1/transactions**
3. Click **"Try it out"**
4. Enter request body:
```json
{
  "amount": 150.50,
  "type": "PAYMENT",
  "description": "Test payment for office supplies"
}
```
5. Click **"Execute"**

**Expected Response:**
```json
{
  "id": 71,
  "amount": 150.50,
  "type": "PAYMENT",
  "status": "COMPLETED",
  "fraudScore": null,
  "createdAt": "2026-01-24T12:45:00Z",
  ...
}
```

✅ Transaction created! Note the ID (e.g., 71)

#### 8.2 Now Test Fraud Detection on Your New Transaction
1. Go back to **POST /api/v1/fraud/detect/{transactionId}**
2. Enter the new transaction ID: `71`
3. Click **"Execute"**
4. See the AI analyze your transaction in real-time!

---

### Step 9: Create Suspicious Transaction

#### 9.1 Create High-Risk Transaction
1. In **"Transaction Management"** controller
2. Click **POST /api/v1/transactions**
3. Click **"Try it out"**
4. Enter request body (high amount):
```json
{
  "amount": 45000.00,
  "type": "WITHDRAWAL",
  "description": "Large urgent cash withdrawal"
}
```
5. Click **"Execute"**

**Expected Response:**
```json
{
  "id": 72,
  "amount": 45000.00,
  "type": "WITHDRAWAL",
  "status": "COMPLETED",
  ...
}
```

#### 9.2 Test Fraud Detection
1. **POST /api/v1/fraud/detect/72**
2. Click **"Execute"**

**Expected Result:** Should flag as **HIGH RISK** or **FRAUD**! 🚨

---

### Step 10: Advanced Testing

#### 10.1 Get High-Risk Transactions
1. Click **GET /api/v1/fraud/high-risk**
2. Click **"Try it out"**
3. Set `threshold`: 0.7
4. Click **"Execute"**

Shows all transactions with fraud score > 0.7

#### 10.2 Get Fraud Trends
1. Click **GET /api/v1/fraud/trends**
2. Set date range
3. Click **"Execute"**

Shows fraud detection trends over time

---

## Testing Scenarios

### Scenario 1: Normal Business Day
```
1. Login as analyst1@financial.tn
2. View transactions (IDs 1-50)
3. Run fraud detection on ID 25
4. Result: Low risk ✅
```

### Scenario 2: Suspicious Activity
```
1. Login as admin1@financial.tn
2. View transaction ID 55 (large amount, 2 AM)
3. Run fraud detection
4. Result: High risk 🚨
5. Check fraud patterns
```

### Scenario 3: Real-Time Detection
```
1. Login as sme1@financial.tn
2. Create transaction: $500 payment
3. Immediately run fraud detection
4. Result: Low risk ✅
5. Create transaction: $50,000 withdrawal
6. Run fraud detection
7. Result: High risk 🚨
```

### Scenario 4: Batch Analysis
```
1. Login as auditor1@financial.tn
2. Batch detect: IDs [51, 52, 53, 54, 55]
3. View statistics
4. Review high-risk transactions
```

---

## Expected Fraud Detection Behavior

### Low Risk Transactions (IDs 1-50):
- **Amount**: $10 - $2,000
- **Time**: 9 AM - 5 PM
- **Fraud Score**: 0.1 - 0.3
- **AI Prediction**: NOT FRAUD ✅
- **Models Agreement**: All 3 models agree

### High Risk Transactions (IDs 51-60):
- **Amount**: $15,000 - $50,000
- **Time**: 11 PM - 5 AM
- **Fraud Score**: 0.6 - 0.95
- **AI Prediction**: FRAUD 🚨
- **Models Agreement**: All 3 models agree

---

## Troubleshooting

### Issue: 401 Unauthorized
**Solution**: Your JWT token expired. Re-login and update authorization.

### Issue: 403 Forbidden
**Solution**: Your user role doesn't have permission. Use ADMIN, ANALYST, or AUDITOR.

### Issue: 404 Transaction Not Found
**Solution**: Check transaction ID exists. View all transactions first.

### Issue: Model returns 0.5 confidence
**Solution**: Transaction is borderline. Features are ambiguous. Normal behavior.

---

## Key Endpoints Summary

| Endpoint | Method | Auth Required | Purpose |
|----------|--------|---------------|---------|
| `/api/v1/auth/login` | POST | No | Get JWT token |
| `/api/v1/transactions` | GET | ADMIN/ANALYST/AUDITOR | List all transactions |
| `/api/v1/transactions` | POST | All authenticated | Create transaction |
| `/api/v1/fraud/detect/{id}` | POST | ADMIN/ANALYST/AUDITOR | **MAIN: Detect fraud** |
| `/api/v1/fraud/batch-detect` | POST | ADMIN/ANALYST/AUDITOR | Batch detection |
| `/api/v1/fraud/statistics` | GET | ADMIN/ANALYST/AUDITOR | Fraud statistics |
| `/api/v1/fraud/patterns` | GET | ADMIN/ANALYST/AUDITOR | Fraud patterns |
| `/api/v1/fraud/high-risk` | GET | ADMIN/ANALYST/AUDITOR | High-risk list |

---

## Success Criteria

✅ **Authentication works**: JWT token obtained  
✅ **Transactions visible**: Can list 70 transactions  
✅ **Fraud detection runs**: Returns prediction with 3 models  
✅ **ONNX model works**: Shows actual ML predictions  
✅ **Legitimate transactions**: Correctly identified as low risk  
✅ **Suspicious transactions**: Correctly flagged as high risk  
✅ **Real-time creation**: New transactions can be analyzed immediately  

---

## Next Steps After Testing

1. **Review model accuracy**: Check if predictions match expectations
2. **Test edge cases**: Transactions near thresholds
3. **Monitor performance**: Check response times
4. **Analyze patterns**: Review detected fraud patterns
5. **Train better models**: Use real production data if needed

Happy Testing! 🎉
