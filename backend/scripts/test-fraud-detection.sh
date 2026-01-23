#!/bin/bash

# Financial Fraud Detection System - Testing Script
# This script automates testing of all major features

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/api/v1"

# Global variables to store tokens
ADMIN_TOKEN=""
ANALYST_TOKEN=""
AUDITOR_TOKEN=""
SME_TOKEN=""
TRANSACTION_ID=""

# Helper functions
print_header() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if application is running
check_application() {
    print_header "Checking Application Status"
    if curl -s "$BASE_URL/actuator/health" > /dev/null; then
        print_success "Application is running at $BASE_URL"
    else
        print_error "Application is not running at $BASE_URL"
        print_info "Please start the application first: docker compose up -d"
        exit 1
    fi
}

# Register users for testing
register_users() {
    print_header "Registering Test Users"
    
    # Register ADMIN
    print_info "Registering ADMIN user..."
    curl -s -X POST "$API_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "admin@test.tn",
            "password": "Admin123!",
            "firstName": "Admin",
            "lastName": "User",
            "role": "ADMIN"
        }' > /dev/null 2>&1 || echo "Admin might already exist"
    
    # Register FINANCIAL_ANALYST
    print_info "Registering FINANCIAL_ANALYST user..."
    curl -s -X POST "$API_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "analyst@test.tn",
            "password": "Analyst123!",
            "firstName": "Financial",
            "lastName": "Analyst",
            "role": "FINANCIAL_ANALYST"
        }' > /dev/null 2>&1 || echo "Analyst might already exist"
    
    # Register AUDITOR
    print_info "Registering AUDITOR user..."
    curl -s -X POST "$API_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "auditor@test.tn",
            "password": "Auditor123!",
            "firstName": "Compliance",
            "lastName": "Auditor",
            "role": "AUDITOR"
        }' > /dev/null 2>&1 || echo "Auditor might already exist"
    
    # Register SME_USER
    print_info "Registering SME_USER..."
    curl -s -X POST "$API_URL/auth/register" \
        -H "Content-Type: application/json" \
        -d '{
            "email": "sme@test.tn",
            "password": "Sme123!",
            "firstName": "Business",
            "lastName": "Owner",
            "role": "SME_USER"
        }' > /dev/null 2>&1 || echo "SME user might already exist"
    
    print_success "Test users registered (or already exist)"
}

# Login and get tokens
login_users() {
    print_header "Logging In Test Users"
    
    # Login as ADMIN
    print_info "Logging in as ADMIN..."
    RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email": "admin@test.tn", "password": "Admin123!"}')
    ADMIN_TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -n "$ADMIN_TOKEN" ]; then
        print_success "ADMIN logged in successfully"
    else
        print_error "Failed to login as ADMIN"
        echo "Response: $RESPONSE"
        exit 1
    fi
    
    # Login as FINANCIAL_ANALYST
    print_info "Logging in as FINANCIAL_ANALYST..."
    RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email": "analyst@test.tn", "password": "Analyst123!"}')
    ANALYST_TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -n "$ANALYST_TOKEN" ]; then
        print_success "ANALYST logged in successfully"
    else
        print_error "Failed to login as ANALYST"
    fi
    
    # Login as AUDITOR
    print_info "Logging in as AUDITOR..."
    RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email": "auditor@test.tn", "password": "Auditor123!"}')
    AUDITOR_TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -n "$AUDITOR_TOKEN" ]; then
        print_success "AUDITOR logged in successfully"
    else
        print_error "Failed to login as AUDITOR"
    fi
    
    # Login as SME_USER
    print_info "Logging in as SME_USER..."
    RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email": "sme@test.tn", "password": "Sme123!"}')
    SME_TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    if [ -n "$SME_TOKEN" ]; then
        print_success "SME_USER logged in successfully"
    else
        print_error "Failed to login as SME_USER"
    fi
}

# Test 1: Normal transaction (SME User)
test_normal_transaction() {
    print_header "Test 1: Normal Transaction (SME User)"
    
    print_info "Creating normal transaction (2500 TND)..."
    RESPONSE=$(curl -s -X POST "$API_URL/transactions" \
        -H "Authorization: Bearer $SME_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "type": "PAYMENT",
            "amount": 2500.00,
            "description": "Monthly supplier payment"
        }')
    
    TRANSACTION_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
    FRAUD_SCORE=$(echo $RESPONSE | grep -o '"fraudScore":[0-9.]*' | cut -d':' -f2)
    STATUS=$(echo $RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)
    
    if [ -n "$TRANSACTION_ID" ]; then
        print_success "Transaction created: ID=$TRANSACTION_ID"
        print_info "Fraud Score: $FRAUD_SCORE"
        print_info "Status: $STATUS"
        
        if (( $(echo "$FRAUD_SCORE < 0.7" | bc -l) )); then
            print_success "Fraud score is low (not flagged as fraud)"
        else
            print_error "Unexpected: Normal transaction flagged as fraud"
        fi
    else
        print_error "Failed to create transaction"
        echo "Response: $RESPONSE"
    fi
}

# Test 2: High-risk transaction
test_high_risk_transaction() {
    print_header "Test 2: High-Risk Transaction"
    
    print_info "Creating high-risk transaction (4800 TND, close to limit)..."
    RESPONSE=$(curl -s -X POST "$API_URL/transactions" \
        -H "Authorization: Bearer $SME_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "type": "WITHDRAWAL",
            "amount": 4800.00,
            "description": "Urgent withdrawal"
        }')
    
    HR_TRANSACTION_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
    HR_FRAUD_SCORE=$(echo $RESPONSE | grep -o '"fraudScore":[0-9.]*' | cut -d':' -f2)
    HR_STATUS=$(echo $RESPONSE | grep -o '"status":"[^"]*' | cut -d'"' -f4)
    
    if [ -n "$HR_TRANSACTION_ID" ]; then
        print_success "Transaction created: ID=$HR_TRANSACTION_ID"
        print_info "Fraud Score: $HR_FRAUD_SCORE"
        print_info "Status: $HR_STATUS"
        
        if (( $(echo "$HR_FRAUD_SCORE > 0.5" | bc -l) )); then
            print_success "Fraud score is elevated (high-risk detected)"
        else
            print_error "Expected higher fraud score for risky transaction"
        fi
    else
        print_error "Failed to create transaction"
        echo "Response: $RESPONSE"
    fi
}

# Test 3: Exceed SME limit
test_exceed_limit() {
    print_header "Test 3: SME User Exceeding Limit"
    
    print_info "Attempting transaction above SME limit (6500 TND > 5000 limit)..."
    RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$API_URL/transactions" \
        -H "Authorization: Bearer $SME_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{
            "type": "PAYMENT",
            "amount": 6500.00,
            "description": "Large equipment purchase"
        }')
    
    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d':' -f2)
    
    if [ "$HTTP_CODE" == "400" ]; then
        print_success "Transaction correctly rejected (limit protection working)"
        print_info "HTTP Code: 400 Bad Request"
    else
        print_error "Expected HTTP 400 for exceeding limit, got: $HTTP_CODE"
        echo "Response: $RESPONSE"
    fi
}

# Test 4: AI Fraud Detection
test_ai_fraud_detection() {
    print_header "Test 4: AI Fraud Detection (Analyst)"
    
    if [ -z "$TRANSACTION_ID" ]; then
        print_error "No transaction ID available. Skipping AI detection test."
        return
    fi
    
    print_info "Running AI fraud detection on transaction $TRANSACTION_ID..."
    RESPONSE=$(curl -s -X POST "$API_URL/fraud/detect/$TRANSACTION_ID" \
        -H "Authorization: Bearer $ANALYST_TOKEN")
    
    IS_FRAUD=$(echo $RESPONSE | grep -o '"isFraud":[a-z]*' | cut -d':' -f2)
    CONFIDENCE=$(echo $RESPONSE | grep -o '"confidence":[0-9.]*' | cut -d':' -f2 | head -1)
    
    if [ -n "$CONFIDENCE" ]; then
        print_success "AI fraud detection completed"
        print_info "Is Fraud: $IS_FRAUD"
        print_info "Confidence: $CONFIDENCE"
        print_info "All 3 AI models ran successfully"
        
        # Extract individual model predictions
        echo ""
        echo "Model Predictions:"
        echo "$RESPONSE" | grep -o '"modelName":"[^"]*","confidence":[0-9.]*' | \
        while IFS= read -r line; do
            MODEL=$(echo $line | cut -d'"' -f4)
            CONF=$(echo $line | grep -o 'confidence":[0-9.]*' | cut -d':' -f2)
            echo "  - $MODEL: $CONF"
        done
    else
        print_error "AI fraud detection failed"
        echo "Response: $RESPONSE"
    fi
}

# Test 5: View fraud patterns
test_fraud_patterns() {
    print_header "Test 5: Fraud Patterns (Analyst)"
    
    print_info "Retrieving fraud patterns..."
    RESPONSE=$(curl -s -X GET "$API_URL/fraud/patterns?size=5" \
        -H "Authorization: Bearer $ANALYST_TOKEN")
    
    if echo "$RESPONSE" | grep -q '"content"'; then
        print_success "Fraud patterns retrieved successfully"
        
        # Count patterns
        PATTERN_COUNT=$(echo $RESPONSE | grep -o '"id":[0-9]*' | wc -l)
        print_info "Found $PATTERN_COUNT fraud patterns"
    else
        print_error "Failed to retrieve fraud patterns"
        echo "Response: $RESPONSE"
    fi
    
    # Get patterns for specific transaction
    if [ -n "$TRANSACTION_ID" ]; then
        print_info "Getting patterns for transaction $TRANSACTION_ID..."
        RESPONSE=$(curl -s -X GET "$API_URL/fraud/patterns/transaction/$TRANSACTION_ID" \
            -H "Authorization: Bearer $ANALYST_TOKEN")
        
        PATTERN_COUNT=$(echo $RESPONSE | grep -o '"id":[0-9]*' | wc -l)
        print_info "Transaction has $PATTERN_COUNT fraud patterns"
    fi
}

# Test 6: Review fraud pattern
test_review_pattern() {
    print_header "Test 6: Review Fraud Pattern (Auditor)"
    
    print_info "Getting unreviewed patterns..."
    RESPONSE=$(curl -s -X GET "$API_URL/fraud/patterns/unreviewed?size=1" \
        -H "Authorization: Bearer $AUDITOR_TOKEN")
    
    PATTERN_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | cut -d':' -f2 | head -1)
    
    if [ -n "$PATTERN_ID" ]; then
        print_info "Found unreviewed pattern: ID=$PATTERN_ID"
        print_info "Marking pattern as reviewed..."
        
        RESPONSE=$(curl -s -X PUT "$API_URL/fraud/patterns/$PATTERN_ID/review" \
            -H "Authorization: Bearer $AUDITOR_TOKEN" \
            -H "Content-Type: application/json" \
            -d '{
                "reviewNotes": "Automated test review - pattern verified"
            }')
        
        if echo "$RESPONSE" | grep -q "successfully"; then
            print_success "Pattern marked as reviewed"
        else
            print_error "Failed to review pattern"
        fi
    else
        print_info "No unreviewed patterns found"
    fi
}

# Test 7: Permission denial
test_permissions() {
    print_header "Test 7: Permission Checks"
    
    # Test 1: SME user tries to view all transactions
    print_info "Test: SME user tries to view all transactions..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$API_URL/transactions" \
        -H "Authorization: Bearer $SME_TOKEN")
    
    if [ "$HTTP_CODE" == "403" ]; then
        print_success "Access correctly denied (403 Forbidden)"
    else
        print_error "Expected 403, got: $HTTP_CODE"
    fi
    
    # Test 2: SME user tries to run fraud detection
    print_info "Test: SME user tries to run fraud detection..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL/fraud/detect/1" \
        -H "Authorization: Bearer $SME_TOKEN")
    
    if [ "$HTTP_CODE" == "403" ]; then
        print_success "Access correctly denied (403 Forbidden)"
    else
        print_error "Expected 403, got: $HTTP_CODE"
    fi
    
    # Test 3: Analyst tries to update AI model
    print_info "Test: Analyst tries to update AI model (ADMIN only)..."
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$API_URL/fraud/models/DJL/update" \
        -H "Authorization: Bearer $ANALYST_TOKEN")
    
    if [ "$HTTP_CODE" == "403" ]; then
        print_success "Access correctly denied (403 Forbidden - ADMIN only)"
    else
        print_error "Expected 403, got: $HTTP_CODE"
    fi
}

# Test 8: Transaction statistics
test_statistics() {
    print_header "Test 8: Transaction Statistics"
    
    print_info "Retrieving transaction statistics..."
    RESPONSE=$(curl -s -X GET "$API_URL/transactions/statistics" \
        -H "Authorization: Bearer $ANALYST_TOKEN")
    
    if echo "$RESPONSE" | grep -q "totalTransactions"; then
        print_success "Statistics retrieved successfully"
        
        TOTAL=$(echo $RESPONSE | grep -o '"totalTransactions":[0-9]*' | cut -d':' -f2)
        print_info "Total transactions in system: $TOTAL"
    else
        print_error "Failed to retrieve statistics"
        echo "Response: $RESPONSE"
    fi
}

# Main test flow
main() {
    print_header "Financial Fraud Detection System - Automated Testing"
    
    echo "This script will test all major features of the system."
    echo "Make sure the application is running: docker compose up -d"
    echo ""
    read -p "Press Enter to continue..."
    
    check_application
    register_users
    login_users
    
    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}  Starting Feature Tests${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════${NC}"
    
    test_normal_transaction
    test_high_risk_transaction
    test_exceed_limit
    test_ai_fraud_detection
    test_fraud_patterns
    test_review_pattern
    test_permissions
    test_statistics
    
    print_header "Testing Complete!"
    print_success "All tests executed"
    print_info "Check the results above for any failures"
    print_info "View Swagger UI: $BASE_URL/swagger-ui/index.html"
}

# Run main if script is executed directly
if [ "$0" == "${BASH_SOURCE[0]}" ]; then
    main
fi
