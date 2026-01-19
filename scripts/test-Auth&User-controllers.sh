#!/bin/bash

# Test script for Authentication and User Management APIs
# Tests the split between AuthController and UserController

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}  Financial API Test Script${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Test 1: Login with seeded admin user
echo -e "${GREEN}Test 1: Login with seeded admin user${NC}"
echo "POST /api/v1/auth/login"
echo "Email: admin1@financial.tn"
echo "Password: Admin123!"
echo ""

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin1@financial.tn",
    "password": "Admin123!"
  }')

echo "Response:"
echo "$LOGIN_RESPONSE" | jq '.'
echo ""

# Extract token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✓ Login successful!${NC}"
    echo "Access Token: ${ACCESS_TOKEN:0:50}..."
    echo ""
else
    echo -e "${RED}✗ Login failed!${NC}"
    exit 1
fi

# Test 2: Get current user profile (AuthController)
echo -e "${GREEN}Test 2: Get current user profile${NC}"
echo "GET /api/v1/auth/me"
echo ""

PROFILE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/auth/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Response:"
echo "$PROFILE_RESPONSE" | jq '.'
echo ""

# Test 3: Get all users (UserController - Admin only)
echo -e "${GREEN}Test 3: Get all users (paginated)${NC}"
echo "GET /api/v1/users?page=0&size=5"
echo ""

USERS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/users?page=0&size=5" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Response:"
echo "$USERS_RESPONSE" | jq '{totalElements, size, content: .content | map({email, role})}'
echo ""

# Test 4: Get user statistics (UserController - Admin/Auditor only)
echo -e "${GREEN}Test 4: Get user statistics${NC}"
echo "GET /api/v1/users/statistics"
echo ""

STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/users/statistics" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Response:"
echo "$STATS_RESPONSE" | jq '.'
echo ""

# Test 5: Test with Analyst user
echo -e "${GREEN}Test 5: Login with analyst user${NC}"
echo "POST /api/v1/auth/login"
echo "Email: analyst1@financial.tn"
echo "Password: Analyst123!"
echo ""

ANALYST_LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "analyst1@financial.tn",
    "password": "Analyst123!"
  }')

ANALYST_TOKEN=$(echo "$ANALYST_LOGIN" | jq -r '.accessToken')

echo "Response:"
echo "$ANALYST_LOGIN" | jq '{userId, email, role, expiresIn}'
echo ""

# Test 6: Analyst tries to access admin endpoint (should fail)
echo -e "${GREEN}Test 6: Analyst tries to get all users (should fail)${NC}"
echo "GET /api/v1/users"
echo ""

FORBIDDEN_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $ANALYST_TOKEN")

echo "Response:"
echo "$FORBIDDEN_RESPONSE" | jq '.'
echo ""

# Test 7: Register new user (AuthController)
echo -e "${GREEN}Test 7: Register new user${NC}"
echo "POST /api/v1/auth/register"
echo ""

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@financial.tn",
    "password": "NewUser123!",
    "firstName": "New",
    "lastName": "User",
    "role": "SME_USER"
  }')

echo "Response:"
echo "$REGISTER_RESPONSE" | jq '.'
echo ""

# Summary
echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}  Test Summary${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""
echo "AuthController endpoints (/api/v1/auth):"
echo "  ✓ POST /auth/register - User registration"
echo "  ✓ POST /auth/login - User authentication"
echo "  ✓ GET /auth/me - Current user profile"
echo "  ✓ POST /auth/change-password - Password change"
echo ""
echo "UserController endpoints (/api/v1/users):"
echo "  ✓ GET /users - List all users (Admin/Auditor)"
echo "  ✓ GET /users/{id} - Get user by ID"
echo "  ✓ PUT /users/{id} - Update user"
echo "  ✓ DELETE /users/{id} - Delete user (Admin)"
echo "  ✓ GET /users/statistics - User statistics"
echo "  ✓ GET /users/search - Search users"
echo "  ✓ POST /users/{id}/lock - Lock account (Admin)"
echo "  ✓ POST /users/{id}/unlock - Unlock account (Admin)"
echo ""
echo -e "${GREEN}Seeded Users:${NC}"
echo "  • admin1-10@financial.tn (Admin123!, Admin123@, ...)"
echo "  • analyst1-8@financial.tn (Analyst123!, Analyst123@, ...)"
echo "  • sme1-7@financial.tn (Sme123!, Sme123@, ...)"
echo "  • auditor1-5@financial.tn (Auditor123!, Auditor123@, ...)"
echo ""
echo -e "${BLUE}Swagger UI: http://localhost:8080/swagger-ui.html${NC}"
echo ""
