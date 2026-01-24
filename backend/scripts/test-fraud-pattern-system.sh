#!/bin/bash

# Fraud Pattern System - Comprehensive Test
# Tests the improved fraud pattern detection and categorization

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080/api/v1"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Fraud Pattern System - Comprehensive Test          ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo ""

# Step 1: Health Check
echo -e "${YELLOW}[Step 1/8]${NC} Checking backend health..."
HEALTH=$(curl -s "http://localhost:8080/actuator/health" | python3 -c "import sys, json; print(json.load(sys.stdin)['status'])")
if [ "$HEALTH" == "UP" ]; then
    echo -e "${GREEN}✓${NC} Backend is healthy"
else
    echo -e "${RED}✗${NC} Backend is not healthy"
    exit 1
fi
echo ""

# Step 2: Login
echo -e "${YELLOW}[Step 2/8]${NC} Logging in as admin..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin1@financial.tn","password":"Admin123!"}')

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" 2>/dev/null || echo "")

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗${NC} Login failed"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓${NC} Logged in successfully"
echo ""

# Step 3: Clear existing patterns (for clean test)
echo -e "${YELLOW}[Step 3/8]${NC} Checking existing fraud patterns..."
EXISTING=$(curl -s "$BASE_URL/fraud/patterns?size=100" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys, json; print(json.load(sys.stdin)['totalElements'])")

echo -e "${BLUE}ℹ${NC} Found $EXISTING existing pattern(s)"
echo ""

# Step 4: Test fraud detection on multiple transactions
echo -e "${YELLOW}[Step 4/8]${NC} Running fraud detection on test transactions..."
echo ""

# Test cases with different risk levels
declare -a TEST_IDS=(25 35 45 55 60 65 70)
declare -A DESCRIPTIONS=(
    [25]="Low-risk transaction"
    [35]="Normal transaction"
    [45]="Medium amount transaction"
    [55]="High amount suspicious transaction"
    [60]="High-risk late night transaction"
    [65]="Weekend transaction"
    [70]="Large withdrawal"
)

for TX_ID in "${TEST_IDS[@]}"; do
    echo -e "${BLUE}  Testing Transaction #$TX_ID${NC} - ${DESCRIPTIONS[$TX_ID]}"
    
    RESULT=$(curl -s -X POST "$BASE_URL/fraud/detect/$TX_ID" \
        -H "Authorization: Bearer $TOKEN")
    
    IS_FRAUD=$(echo $RESULT | python3 -c "import sys, json; print(json.load(sys.stdin)['isFraud'])")
    CONFIDENCE=$(echo $RESULT | python3 -c "import sys, json; print(f\"{json.load(sys.stdin)['confidence']:.3f}\")")
    REASON=$(echo $RESULT | python3 -c "import sys, json; print(json.load(sys.stdin)['primaryReason'][:60])")
    
    if [ "$IS_FRAUD" == "True" ]; then
        echo -e "    ${RED}⚠ FRAUD DETECTED${NC} - Confidence: $CONFIDENCE"
    else
        echo -e "    ${GREEN}✓ No fraud${NC} - Confidence: $CONFIDENCE"
    fi
    echo -e "    ${YELLOW}→${NC} $REASON..."
    echo ""
    
    sleep 0.5
done

# Step 5: Retrieve all patterns
echo -e "${YELLOW}[Step 5/8]${NC} Retrieving stored fraud patterns..."
PATTERNS=$(curl -s "$BASE_URL/fraud/patterns?size=20&sort=confidence,desc" \
    -H "Authorization: Bearer $TOKEN")

TOTAL=$(echo $PATTERNS | python3 -c "import sys, json; print(json.load(sys.stdin)['totalElements'])")
echo -e "${GREEN}✓${NC} Found $TOTAL fraud pattern(s)"
echo ""

# Display patterns in table format
echo "┌──────────────────────────────────────────────────────────────────────────────┐"
echo "│                           Fraud Patterns Summary                              │"
echo "├────┬────────────────────────────┬────────────┬────────┬─────────────────────────┤"
echo "│ ID │ Pattern Type               │ Confidence │ TX ID  │ Detected                │"
echo "├────┼────────────────────────────┼────────────┼────────┼─────────────────────────┤"

echo "$PATTERNS" | python3 << 'EOF'
import sys, json
try:
    data = json.load(sys.stdin)
    if data['totalElements'] > 0:
        for p in data['content'][:10]:
            print(f"│ {p['id']:2d} │ {p['patternType'][:26]:26s} │ {p['confidence']:10.3f} │ {p['transactionId']:6d} │ {p['detectedAt'][:19]:23s} │")
    else:
        print("│                           No patterns found                               │")
except Exception as e:
    print(f"│ Error: {str(e)[:72]:72s} │")
print("└────┴────────────────────────────┴────────────┴────────┴─────────────────────────┘")
EOF

echo ""

# Step 6: Analyze pattern types
echo -e "${YELLOW}[Step 6/8]${NC} Analyzing pattern type distribution..."
echo ""

echo "$PATTERNS" | python3 << 'EOF'
import sys, json
from collections import Counter

try:
    data = json.load(sys.stdin)
    if data['totalElements'] > 0:
        pattern_types = [p['patternType'] for p in data['content']]
        counter = Counter(pattern_types)
        
        print("Pattern Type Distribution:")
        for pattern_type, count in counter.most_common():
            bar = '█' * (count * 5)
            print(f"  {pattern_type:30s} {bar} {count}")
    else:
        print("  No patterns to analyze")
except Exception as e:
    print(f"  Error analyzing patterns: {e}")
EOF

echo ""

# Step 7: Get unreviewed patterns
echo -e "${YELLOW}[Step 7/8]${NC} Checking unreviewed patterns..."
UNREVIEWED=$(curl -s "$BASE_URL/fraud/patterns/unreviewed?size=100" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys, json; print(json.load(sys.stdin)['totalElements'])")

echo -e "${YELLOW}⚠${NC} $UNREVIEWED pattern(s) need review"
echo ""

# Step 8: Get high-confidence patterns
echo -e "${YELLOW}[Step 8/8]${NC} Querying high-confidence patterns (>0.6)..."
HIGH_CONF=$(curl -s "$BASE_URL/fraud/patterns/high-confidence?threshold=0.6" \
    -H "Authorization: Bearer $TOKEN" \
    | python3 -c "import sys, json; print(len(json.load(sys.stdin)))")

echo -e "${RED}⚠${NC} $HIGH_CONF high-risk pattern(s) detected"
echo ""

# Summary
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                     Test Summary                        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Transactions Tested:     ${GREEN}${#TEST_IDS[@]}${NC}"
echo -e "  Total Patterns Stored:   ${YELLOW}$TOTAL${NC}"
echo -e "  Unreviewed Patterns:     ${YELLOW}$UNREVIEWED${NC}"
echo -e "  High-Risk Patterns:      ${RED}$HIGH_CONF${NC}"
echo ""
echo -e "${GREEN}✓ Fraud pattern system test completed successfully!${NC}"
echo ""

# Additional info
echo -e "${BLUE}ℹ Additional Commands:${NC}"
echo ""
echo "# Get patterns for specific transaction:"
echo "curl \"$BASE_URL/fraud/patterns/transaction/55\" -H \"Authorization: Bearer \$TOKEN\""
echo ""
echo "# Get patterns by date range:"
echo "curl \"$BASE_URL/fraud/patterns/date-range?startDate=2024-01-01&endDate=2024-12-31\" -H \"Authorization: Bearer \$TOKEN\""
echo ""
echo "# Get pattern metadata:"
echo "curl \"$BASE_URL/fraud/patterns/1\" -H \"Authorization: Bearer \$TOKEN\" | python3 -m json.tool"
echo ""
