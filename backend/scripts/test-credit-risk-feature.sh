#!/bin/bash

# Test script for Credit Risk Assessment APIs using Admin user

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/v1"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Credit Risk Feature Test Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 0: Health check
echo -e "${GREEN}Step 0: Backend health check${NC}"
if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
  STATUS=$(curl -s "$BASE_URL/actuator/health" | jq -r '.status // .components.health.status // "UNKNOWN"')
  echo "Health: $STATUS"
else
  echo -e "${RED}✗ Backend not reachable at $BASE_URL${NC}"
  exit 1
fi

echo ""

# Step 1: Login as Admin
echo -e "${GREEN}Step 1: Login as Admin user${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin1@financial.tn",
    "password": "Admin123!"
  }')

echo "Response:"
echo "$LOGIN_RESPONSE" | jq '{userId, email, role, token: .token}'
echo ""

ADMIN_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}✗ Admin login failed${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Admin login successful${NC}"
echo "Token (first 50 chars): ${ADMIN_TOKEN:0:50}..."
echo ""

# Step 2: Find one SME user with existing credit risk assessments
echo -e "${GREEN}Step 2: Find SME user for credit risk tests${NC}"
USERS_PAGE=$(curl -s -X GET "$API_BASE/users?page=0&size=50" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

SME_USER=$(echo "$USERS_PAGE" | jq -r '.content[] | select(.role == "SME_USER") | {id, email} | @json' | head -n 1)

if [ -z "$SME_USER" ]; then
  echo -e "${RED}✗ No SME_USER found in users list${NC}"
  exit 1
fi

SME_USER_ID=$(echo "$SME_USER" | jq -r '.id')
SME_EMAIL=$(echo "$SME_USER" | jq -r '.email')

echo -e "${GREEN}✓ Using SME user: $SME_EMAIL ($SME_USER_ID)${NC}"
echo ""

# Step 3: List credit risk assessments for this SME user
echo -e "${GREEN}Step 3: Get credit risk assessments for SME user${NC}"
ASSESSMENTS=$(curl -s -X GET "$API_BASE/credit-risk/user/$SME_USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$ASSESSMENTS" | jq 'map({id, riskCategory, riskScore, assessedAt})' || echo "$ASSESSMENTS"
echo ""

ASSESSMENT_ID=$(echo "$ASSESSMENTS" | jq -r '.[0].id // empty')

if [ -z "$ASSESSMENT_ID" ]; then
  echo -e "${RED}✗ No credit risk assessments found for SME user${NC}"
  exit 1
fi

echo -e "${GREEN}✓ Using assessment ID: $ASSESSMENT_ID${NC}"
echo ""

# Step 4: Get assessment by ID
echo -e "${GREEN}Step 4: Get assessment by ID${NC}"
ASSESSMENT_BY_ID=$(curl -s -X GET "$API_BASE/credit-risk/$ASSESSMENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$ASSESSMENT_BY_ID" | jq '{id, riskCategory, riskScore, industrySector, assessedAt, reviewed}'
echo ""

# Step 5: Generate detailed risk report
echo -e "${GREEN}Step 5: Generate risk report${NC}"
RISK_REPORT=$(curl -s -X GET "$API_BASE/credit-risk/report/$ASSESSMENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response (summary):"
echo "$RISK_REPORT" | jq '{assessmentId, overallRiskCategory, overallRiskScore, keyFindings: (.keyFindings[0:3])}'
echo ""

# Step 6: Global credit risk statistics
echo -e "${GREEN}Step 6: Get global credit risk statistics${NC}"
RISK_STATS=$(curl -s -X GET "$API_BASE/credit-risk/statistics" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$RISK_STATS" | jq '.'
echo ""

# Step 7: Average risk score for SME user
echo -e "${GREEN}Step 7: Get average risk score for SME user${NC}"
USER_AVG=$(curl -s -X GET "$API_BASE/credit-risk/user/$SME_USER_ID/average-score" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Response:"
echo "$USER_AVG" | jq '.'
echo ""

# Step 8: High-risk and unreviewed-high-risk assessments
echo -e "${GREEN}Step 8: Get high-risk and unreviewed high-risk assessments${NC}"
HIGH_RISK=$(curl -s -X GET "$API_BASE/credit-risk/high-risk?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
UNREVIEWED_HIGH=$(curl -s -X GET "$API_BASE/credit-risk/unreviewed/high-risk?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "High-risk (first few):"
echo "$HIGH_RISK" | jq '{totalElements, content: .content | map({id, riskCategory, riskScore, reviewed})}'
echo ""

echo "Unreviewed high-risk (first few):"
echo "$UNREVIEWED_HIGH" | jq '{totalElements, content: .content | map({id, riskCategory, riskScore, reviewed})}'
echo ""

# Step 9: Sector-based queries using assessment sector
echo -e "${GREEN}Step 9: Sector-based statistics${NC}"
SECTOR=$(echo "$ASSESSMENT_BY_ID" | jq -r '.industrySector // "Technology"')

echo "Using sector: $SECTOR"

SECTOR_ASSESSMENTS=$(curl -s -X GET "$API_BASE/credit-risk/sector/$SECTOR?page=0&size=5" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
SECTOR_AVG=$(curl -s -X GET "$API_BASE/credit-risk/sector/$SECTOR/average-score" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "Sector assessments (first few):"
echo "$SECTOR_ASSESSMENTS" | jq '{totalElements, content: .content | map({id, riskCategory, riskScore, industrySector})}'
echo ""

echo "Sector average score:"
echo "$SECTOR_AVG" | jq '.'
echo ""

# Step 10: Mark assessment as reviewed
echo -e "${GREEN}Step 10: Mark assessment as reviewed${NC}"
REVIEW_RESPONSE=$(curl -s -X PUT "$API_BASE/credit-risk/$ASSESSMENT_ID/review" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reviewNotes": "Reviewed via test script"}')

if [ $? -eq 0 ]; then
  echo -e "${GREEN}✓ Assessment $ASSESSMENT_ID marked as reviewed${NC}"
else
  echo -e "${RED}✗ Failed to mark assessment as reviewed${NC}"
fi

echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Credit Risk Feature Test Completed${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
