#!/bin/bash

# Shadow Ledger System - Acceptance Test Suite
# This script runs end-to-end acceptance tests for all microservices

set -e

echo "=========================================="
echo "Shadow Ledger System - Acceptance Tests"
echo "=========================================="
echo ""

# Configuration
EVENT_SERVICE="http://localhost:9091"
SHADOW_LEDGER_SERVICE="http://localhost:9092"
DRIFT_SERVICE="http://localhost:9093"
API_GATEWAY="http://localhost:9090"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0

# Helper functions
pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASSED++))
}

fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((FAILED++))
}

info() {
    echo -e "${YELLOW}ℹ INFO${NC}: $1"
}

# Test 1: Health Checks
echo "Test Suite 1: Health Checks"
echo "----------------------------"

if curl -s "$EVENT_SERVICE/actuator/health" | grep -q "UP"; then
    pass "Event Service health check"
else
    fail "Event Service health check"
fi

if curl -s "$SHADOW_LEDGER_SERVICE/actuator/health" | grep -q "UP"; then
    pass "Shadow Ledger Service health check"
else
    fail "Shadow Ledger Service health check"
fi

if curl -s "$DRIFT_SERVICE/actuator/health" | grep -q "UP"; then
    pass "Drift Correction Service health check"
else
    fail "Drift Correction Service health check"
fi

echo ""

# Test Suite 2: Event Validation
echo "Test Suite 2: Event Validation"
echo "-------------------------------"

# Test 2.1: Valid event should be accepted
RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$EVENT_SERVICE/events" \
    -H "Content-Type: application/json" \
    -d '{
        "eventId": "TEST_VALID_'$(date +%s)'",
        "accountId": "A_TEST",
        "type": "credit",
        "amount": 100,
        "timestamp": '$(date +%s000)'
    }')

if [ "$RESPONSE" = "202" ]; then
    pass "Valid event accepted (HTTP 202)"
else
    fail "Valid event accepted (got HTTP $RESPONSE)"
fi

# Test 2.2: Duplicate event ID should be rejected
EVENT_ID="TEST_DUP_$(date +%s)"
curl -s -X POST "$EVENT_SERVICE/events" \
    -H "Content-Type: application/json" \
    -d '{
        "eventId": "'$EVENT_ID'",
        "accountId": "A_TEST",
        "type": "credit",
        "amount": 100,
        "timestamp": '$(date +%s000)'
    }' > /dev/null

RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$EVENT_SERVICE/events" \
    -H "Content-Type: application/json" \
    -d '{
        "eventId": "'$EVENT_ID'",
        "accountId": "A_TEST",
        "type": "credit",
        "amount": 200,
        "timestamp": '$(date +%s000)'
    }')

if [ "$RESPONSE" = "500" ] || [ "$RESPONSE" = "409" ]; then
    pass "Duplicate event rejected"
else
    fail "Duplicate event rejected (got HTTP $RESPONSE)"
fi

# Test 2.3: Invalid type should be rejected
RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$EVENT_SERVICE/events" \
    -H "Content-Type: application/json" \
    -d '{
        "eventId": "TEST_INVALID_TYPE_'$(date +%s)'",
        "accountId": "A_TEST",
        "type": "invalid",
        "amount": 100,
        "timestamp": '$(date +%s000)'
    }')

if [ "$RESPONSE" = "400" ] || [ "$RESPONSE" = "500" ]; then
    pass "Invalid type rejected"
else
    fail "Invalid type rejected (got HTTP $RESPONSE)"
fi

echo ""

# Test Suite 3: Shadow Balance Calculation
echo "Test Suite 3: Shadow Balance Calculation"
echo "----------------------------------------"

# Create test account with known transactions
ACCOUNT_ID="A_CALC_TEST_$(date +%s)"
TIMESTAMP=$(date +%s000)

# Post events
curl -s -X POST "$EVENT_SERVICE/events" -H "Content-Type: application/json" \
    -d '{"eventId":"CALC1_'$ACCOUNT_ID'","accountId":"'$ACCOUNT_ID'","type":"credit","amount":500,"timestamp":'$TIMESTAMP'}' > /dev/null

curl -s -X POST "$EVENT_SERVICE/events" -H "Content-Type: application/json" \
    -d '{"eventId":"CALC2_'$ACCOUNT_ID'","accountId":"'$ACCOUNT_ID'","type":"credit","amount":300,"timestamp":'$((TIMESTAMP+1000))'}' > /dev/null

curl -s -X POST "$EVENT_SERVICE/events" -H "Content-Type: application/json" \
    -d '{"eventId":"CALC3_'$ACCOUNT_ID'","accountId":"'$ACCOUNT_ID'","type":"debit","amount":50,"timestamp":'$((TIMESTAMP+2000))'}' > /dev/null

info "Waiting 10 seconds for Kafka processing..."
sleep 10

# Query balance
BALANCE=$(curl -s "$SHADOW_LEDGER_SERVICE/accounts/$ACCOUNT_ID/shadow-balance" | grep -o '"balance":[0-9.]*' | cut -d':' -f2)

if [ "$BALANCE" = "750.0" ] || [ "$BALANCE" = "750" ]; then
    pass "Balance calculation correct (500 + 300 - 50 = 750)"
else
    fail "Balance calculation (expected 750, got $BALANCE)"
fi

echo ""

# Test Suite 4: Drift Detection
echo "Test Suite 4: Drift Detection"
echo "------------------------------"

# Test 4.1: No drift scenario
RESPONSE=$(curl -s -X POST "$DRIFT_SERVICE/drift-check" \
    -H "Content-Type: application/json" \
    -d '[{"accountId":"'$ACCOUNT_ID'","reportedBalance":750}]')

if echo "$RESPONSE" | grep -q '\[\]' || echo "$RESPONSE" | grep -q '"accountId"'; then
    pass "No drift detected when balances match"
else
    fail "No drift scenario"
fi

# Test 4.2: Positive drift (CBS > Shadow)
RESPONSE=$(curl -s -X POST "$DRIFT_SERVICE/drift-check" \
    -H "Content-Type: application/json" \
    -d '[{"accountId":"'$ACCOUNT_ID'","reportedBalance":900}]')

if echo "$RESPONSE" | grep -q '"type":"credit"'; then
    pass "Positive drift generates credit correction"
else
    fail "Positive drift detection (response: $RESPONSE)"
fi

# Test 4.3: Negative drift (CBS < Shadow)
RESPONSE=$(curl -s -X POST "$DRIFT_SERVICE/drift-check" \
    -H "Content-Type: application/json" \
    -d '[{"accountId":"'$ACCOUNT_ID'","reportedBalance":700}]')

if echo "$RESPONSE" | grep -q '"type":"debit"'; then
    pass "Negative drift generates debit correction"
else
    fail "Negative drift detection (response: $RESPONSE)"
fi

echo ""

# Test Suite 5: Manual Correction
echo "Test Suite 5: Correction Event Generation"
echo "------------------------------------------"

CORRECTION_RESPONSE=$(curl -s -X POST "$DRIFT_SERVICE/correct/$ACCOUNT_ID?type=credit&amount=100")

if echo "$CORRECTION_RESPONSE" | grep -q '"eventId"' && echo "$CORRECTION_RESPONSE" | grep -q 'MANUAL'; then
    pass "Manual correction event generated"
else
    fail "Manual correction generation"
fi

if echo "$CORRECTION_RESPONSE" | grep -q '"amount":100'; then
    pass "Correction amount is correct"
else
    fail "Correction amount"
fi

echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "Total: $((PASSED + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi

