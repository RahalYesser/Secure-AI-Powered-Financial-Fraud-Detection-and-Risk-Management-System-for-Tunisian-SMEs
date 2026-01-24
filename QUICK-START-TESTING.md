# 🚀 Quick Start - Fraud Detection Testing

## ✅ System Status
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health
- **Data Seeded**: 30 users + 70 transactions ✅

---

## 🎯 5-Minute Test Flow

### 1️⃣ Open Swagger
```
http://localhost:8080/swagger-ui.html
```

### 2️⃣ Login (Get JWT Token)
**Endpoint**: `POST /api/v1/auth/login`

**Credentials** (choose one):
```json
{
  "email": "admin1@financial.tn",
  "password": "Admin123!"
}
```

**Copy the JWT token** from response!

### 3️⃣ Authorize
1. Click green **"Authorize"** button (top right)
2. Paste your JWT token
3. Click **"Authorize"**, then **"Close"**

### 4️⃣ Test Legitimate Transaction
**Endpoint**: `POST /api/v1/fraud/detect/{transactionId}`
- Enter ID: **`25`** (normal transaction)
- Click **"Execute"**

**Expected**: `isFraud: false`, low confidence (~0.2-0.3)

### 5️⃣ Test Suspicious Transaction
**Same endpoint**
- Enter ID: **`55`** (high-risk transaction)
- Click **"Execute"**

**Expected**: `isFraud: true`, high confidence (~0.7-0.9) 🚨

---

## 📊 Test Data Summary

### Legitimate Transactions (IDs 1-50)
- Amount: $10 - $2,000
- Time: 9 AM - 5 PM (business hours)
- Fraud Score: 0.1 - 0.3
- **Test IDs**: 5, 10, 25, 30, 45

### Suspicious Transactions (IDs 51-60)
- Amount: $15,000 - $50,000
- Time: 11 PM - 5 AM (odd hours)
- Fraud Score: 0.6 - 0.95
- **Test IDs**: 51, 53, 55, 57, 60

### Mixed Transactions (IDs 61-70)
- Various patterns
- **Test IDs**: 61, 65, 70

---

## 🔑 Available User Accounts

| Email | Password | Role |
|-------|----------|------|
| admin1@financial.tn | Admin123! | ADMIN |
| analyst1@financial.tn | Analyst123! | FINANCIAL_ANALYST |
| auditor1@financial.tn | Auditor123! | AUDITOR |
| sme1@financial.tn | Sme123! | SME_USER |

*Note: Numbers go up to admin10, analyst8, auditor5, sme7*

---

## 🧪 Key Test Scenarios

### Scenario A: Normal Business Day
```
1. Login as analyst1@financial.tn
2. Test transaction ID: 25
3. Result: Low risk ✅
```

### Scenario B: Fraud Alert
```
1. Login as admin1@financial.tn
2. Test transaction ID: 55
3. Result: High risk 🚨
4. Check fraud reason in response
```

### Scenario C: Batch Detection
```
1. Endpoint: POST /api/v1/fraud/batch-detect
2. Body: {"transactionIds": [25, 55, 56, 30, 58]}
3. Compare results
```

### Scenario D: Create & Test
```
1. POST /api/v1/transactions
2. Body: {"amount": 150.50, "type": "PAYMENT", "description": "Test"}
3. Note new transaction ID
4. Test fraud detection on new ID
```

---

## 📈 Other Useful Endpoints

### View All Transactions
`GET /api/v1/transactions`

### Get High-Risk Transactions
`GET /api/v1/fraud/high-risk?threshold=0.7`

### Fraud Statistics
`GET /api/v1/fraud/statistics`

### Fraud Patterns
`GET /api/v1/fraud/patterns`

---

## ✅ Success Indicators

You should see **3 AI models** in fraud detection response:
1. **ONNX-Runtime** (trained model - most accurate)
2. **DJL-PyTorch** (rule-based fallback)
3. **TensorFlow-Java** (rule-based fallback)

All 3 should **agree** on fraud/legitimate classification!

---

## 🚨 Troubleshooting

**401 Unauthorized?**
- JWT token expired → Re-login and update authorization

**403 Forbidden?**
- User role doesn't have permission → Use ADMIN, ANALYST, or AUDITOR

**404 Transaction Not Found?**
- Transaction ID doesn't exist → Use IDs 1-70

**Model returns 0.5?**
- Transaction is borderline → Normal behavior for ambiguous cases

---

## 🎉 You're Ready!

Open Swagger → Login → Authorize → Test transactions 25 and 55 → See AI in action!

For detailed documentation, see **TESTING-GUIDE.md**
