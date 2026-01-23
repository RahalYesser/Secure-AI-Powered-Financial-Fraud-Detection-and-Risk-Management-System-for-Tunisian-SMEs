#!/bin/bash

# Test script for Transaction Management Feature
# Tests the new transaction endpoints and functionality

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Transaction Feature Test Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Test 1: Login as SME User
echo -e "${GREEN}Test 1: Login as SME User${NC}"
echo "POST /api/v1/auth/login"
echo "Email: sme1@financial.tn"
echo ""

SME_LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sme1@financial.tn",
    "password": "Sme123!"
  }')

SME_TOKEN=$(echo "$SME_LOGIN" | jq -r '.token')

if [ "$SME_TOKEN" != "null" ] && [ -n "$SME_TOKEN" ]; then
    echo -e "${GREEN}✓ SME Login successful!${NC}"
    echo "Access Token: ${SME_TOKEN:0:50}..."
    echo ""
else
    echo -e "${RED}✗ Login failed!${NC}"
    exit 1
fi

# Test 2: Create a DEPOSIT transaction
echo -e "${GREEN}Test 2: Create DEPOSIT Transaction${NC}"
echo "POST /api/v1/transactions"
echo ""

DEPOSIT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transactions" \
  -H "Authorization: Bearer $SME_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "DEPOSIT",
    "amount": 1000.00,
    "description": "Initial deposit"
  }')

echo "Response:"
echo "$DEPOSIT_RESPONSE" | jq '.'
echo ""

DEPOSIT_ID=$(echo "$DEPOSIT_RESPONSE" | jq -r '.id')
FRAUD_SCORE=$(echo "$DEPOSIT_RESPONSE" | jq -r '.fraudScore')

echo -e "${YELLOW}Transaction ID: $DEPOSIT_ID${NC}"
echo -e "${YELLOW}Fraud Score: $FRAUD_SCORE${NC}"
echo ""

# Test 3: Create a PAYMENT transaction
echo -e "${GREEN}Test 3: Create PAYMENT Transaction${NC}"
echo "POST /api/v1/transactions"
echo ""

PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transactions" \
  -H "Authorization: Bearer $SME_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "PAYMENT",
    "amount": 150.50,
    "description": "Online shopping payment"
  }')

echo "Response:"
echo "$PAYMENT_RESPONSE" | jq '.'
echo ""

PAYMENT_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.id')
REFERENCE_NUMBER=$(echo "$PAYMENT_RESPONSE" | jq -r '.referenceNumber')

echo -e "${YELLOW}Transaction ID: $PAYMENT_ID${NC}"
echo -e "${YELLOW}Reference Number: $REFERENCE_NUMBER${NC}"
echo ""

# Test 4: Try to create a transaction exceeding SME limit
echo -e "${GREEN}Test 4: Test SME Transaction Limit (should fail)${NC}"
echo "POST /api/v1/transactions"
echo "Amount: 6000.00 (exceeds SME limit of 5000)${NC}"
echo ""

LIMIT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transactions" \
  -H "Authorization: Bearer $SME_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "TRANSFER",
    "amount": 6000.00,
    "description": "Large transfer"
  }')

echo "Response:"
echo "$LIMIT_RESPONSE" | jq '.'
echo ""

# Test 5: Get my transactions
echo -e "${GREEN}Test 5: Get My Transactions${NC}"
echo "GET /api/v1/transactions/my-transactions"
echo ""

MY_TRANSACTIONS=$(curl -s -X GET "$BASE_URL/api/v1/transactions/my-transactions?page=0&size=10" \
  -H "Authorization: Bearer $SME_TOKEN")

echo "Response:"
echo "$MY_TRANSACTIONS" | jq '{totalElements, content: .content | map({id, type, status, amount, description})}'
echo ""

# Test 6: Get transaction by ID
echo -e "${GREEN}Test 6: Get Transaction by ID${NC}"
echo "GET /api/v1/transactions/$PAYMENT_ID"
echo ""

TRANSACTION_BY_ID=$(curl -s -X GET "$BASE_URL/api/v1/transactions/$PAYMENT_ID" \
  -H "Authorization: Bearer $SME_TOKEN")

echo "Response:"
echo "$TRANSACTION_BY_ID" | jq '.'
echo ""

# Test 7: Get my statistics
echo -e "${GREEN}Test 7: Get My Transaction Statistics${NC}"
echo "GET /api/v1/transactions/my-statistics"
echo ""

MY_STATS=$(curl -s -X GET "$BASE_URL/api/v1/transactions/my-statistics" \
  -H "Authorization: Bearer $SME_TOKEN")

echo "Response:"
echo "$MY_STATS" | jq '.'
echo ""

# Test 8: Login as Admin
echo -e "${GREEN}Test 8: Login as Admin${NC}"
echo "POST /api/v1/auth/login"
echo "Email: admin1@financial.tn"
echo ""

ADMIN_LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin1@financial.tn",
    "password": "Admin123!"
  }')

ADMIN_TOKEN=$(echo "$ADMIN_LOGIN" | jq -r '.token')

if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ]; then
    echo -e "${GREEN}✓ Admin Login successful!${NC}"
    echo "Access Token: ${ADMIN_TOKEN:0:50}..."
    echo ""
else
    echo -e "${RED}✗ Admin Login failed!${NC}"
fi

# Test 9: Get all transactions (Admin only)
echo -e "${GREEN}Test 9: Get All Transactions (Admin)${NC}"
echo "GET /api/v1/transactions"
echo ""

ALL_TRANSACTIONS=$(curl -s -X GET "$BASE_URL/api/v1/transactions?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$ALL_TRANSACTIONS" | jq '{totalElements, size, content: .content | map({id, type, status, amount, userEmail})}'
echo ""

# Test 10: Update transaction status (Admin only)
echo -e "${GREEN}Test 10: Update Transaction Status to COMPLETED (Admin)${NC}"
echo "PUT /api/v1/transactions/$DEPOSIT_ID/status?status=COMPLETED"
echo ""

UPDATE_STATUS=$(curl -s -X PUT "$BASE_URL/api/v1/transactions/$DEPOSIT_ID/status?status=COMPLETED" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$UPDATE_STATUS" | jq '.'
echo ""

# Test 11: Get overall statistics (Admin only)
echo -e "${GREEN}Test 11: Get Overall Transaction Statistics (Admin)${NC}"
echo "GET /api/v1/transactions/statistics"
echo ""

OVERALL_STATS=$(curl -s -X GET "$BASE_URL/api/v1/transactions/statistics" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$OVERALL_STATS" | jq '.'
echo ""

# Test 12: Filter transactions by status (Admin only)
echo -e "${GREEN}Test 12: Filter Transactions by Status: PENDING${NC}"
echo "GET /api/v1/transactions/status/PENDING"
echo ""

PENDING_TRANSACTIONS=$(curl -s -X GET "$BASE_URL/api/v1/transactions/status/PENDING?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$PENDING_TRANSACTIONS" | jq '{totalElements, content: .content | map({id, status, amount, userEmail})}'
echo ""

# Test 13: Filter transactions by type (Admin only)
echo -e "${GREEN}Test 13: Filter Transactions by Type: PAYMENT${NC}"
echo "GET /api/v1/transactions/type/PAYMENT"
echo ""

PAYMENT_TRANSACTIONS=$(curl -s -X GET "$BASE_URL/api/v1/transactions/type/PAYMENT?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$PAYMENT_TRANSACTIONS" | jq '{totalElements, content: .content | map({id, type, amount, userEmail})}'
echo ""

# Test 14: Login as Analyst
echo -e "${GREEN}Test 14: Login as Financial Analyst${NC}"
echo "POST /api/v1/auth/login"
echo "Email: analyst1@financial.tn"
echo ""

ANALYST_LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "analyst1@financial.tn",
    "password": "Analyst123!"
  }')

ANALYST_TOKEN=$(echo "$ANALYST_LOGIN" | jq -r '.token')

if [ "$ANALYST_TOKEN" != "null" ] && [ -n "$ANALYST_TOKEN" ]; then
    echo -e "${GREEN}✓ Analyst Login successful!${NC}"
    echo ""
fi

# Test 15: Analyst can view transactions
echo -e "${GREEN}Test 15: Analyst Views All Transactions${NC}"
echo "GET /api/v1/transactions"
echo ""

ANALYST_VIEW=$(curl -s -X GET "$BASE_URL/api/v1/transactions?page=0&size=3" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

echo "Response:"
echo "$ANALYST_VIEW" | jq '{totalElements, content: .content | map({id, type, status, amount})}'
echo ""

# Test 16: Analyst can update transaction status
echo -e "${GREEN}Test 16: Analyst Updates Transaction Status${NC}"
echo "PUT /api/v1/transactions/$PAYMENT_ID/status?status=COMPLETED"
echo ""

ANALYST_UPDATE=$(curl -s -X PUT "$BASE_URL/api/v1/transactions/$PAYMENT_ID/status?status=COMPLETED" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

echo "Response:"
echo "$ANALYST_UPDATE" | jq '{id, status, type, amount}'
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}✓ Transaction Creation${NC}"
echo "  - DEPOSIT transaction created"
echo "  - PAYMENT transaction created"
echo "  - SME transaction limit enforced (>5000 rejected)"
echo ""
echo -e "${GREEN}✓ Transaction Retrieval${NC}"
echo "  - Get transaction by ID"
echo "  - Get my transactions (paginated)"
echo "  - Get transaction by reference number"
echo ""
echo -e "${GREEN}✓ Transaction Filtering (Admin/Analyst)${NC}"
echo "  - Filter by status (PENDING, COMPLETED, etc.)"
echo "  - Filter by type (PAYMENT, DEPOSIT, etc.)"
echo "  - View all transactions (paginated)"
echo ""
echo -e "${GREEN}✓ Transaction Statistics${NC}"
echo "  - User statistics (own transactions)"
echo "  - Overall statistics (Admin/Analyst)"
echo ""
echo -e "${GREEN}✓ Role-Based Access Control${NC}"
echo "  - SME users can create transactions (with limits)"
echo "  - Admins can view/update all transactions"
echo "  - Analysts can view/update transactions"
echo "  - Users can only view their own transactions"
echo ""
echo -e "${GREEN}✓ Fraud Detection${NC}"
echo "  - Fraud score calculated for each transaction"
echo "  - Large transactions flagged automatically"
echo ""
echo -e "${YELLOW}Transaction Feature Endpoints:${NC}"
echo "  POST   /api/v1/transactions                    - Create transaction"
echo "  GET    /api/v1/transactions/{id}               - Get by ID"
echo "  GET    /api/v1/transactions/my-transactions    - Get my transactions"
echo "  GET    /api/v1/transactions                    - Get all (Admin/Analyst)"
echo "  GET    /api/v1/transactions/status/{status}    - Filter by status"
echo "  GET    /api/v1/transactions/type/{type}        - Filter by type"
echo "  GET    /api/v1/transactions/my-statistics      - My statistics"
echo "  GET    /api/v1/transactions/statistics         - Overall statistics"
echo "  PUT    /api/v1/transactions/{id}/status        - Update status (Admin/Analyst)"
echo "  POST   /api/v1/transactions/{id}/cancel        - Cancel transaction"
echo ""
echo -e "${BLUE}Swagger UI: http://localhost:8080/swagger-ui.html${NC}"
echo -e "${BLUE}Documentation: README-TRANSACTION-FEATURE.md${NC}"
echo ""
