#!/bin/bash

# Auth Service API Test Script (Linux/Mac)

BASE_URL="http://localhost:8081"

echo "========================================"
echo "  Auth Service API Testing Script"
echo "========================================"
echo ""

# Test 1: Health Check
echo "[1/4] Testing Health Endpoint..."
HEALTH_RESPONSE=$(curl -s "$BASE_URL/auth/health")
if [ $? -eq 0 ]; then
    echo "✓ Health Check: Service is UP"
else
    echo "✗ Health Check Failed"
    echo "Make sure the service is running on port 8081"
    exit 1
fi
echo ""

# Test 2: User Registration
echo "[2/4] Testing User Registration..."
RANDOM_NUM=$RANDOM
REGISTER_DATA=$(cat <<EOF
{
  "username": "testuser_$RANDOM_NUM",
  "email": "testuser_$RANDOM_NUM@example.com",
  "password": "test123456",
  "fullName": "Test User"
}
EOF
)

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "$REGISTER_DATA")

if [ $? -eq 0 ]; then
    echo "✓ Registration Successful"
    TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.token')
    EMAIL=$(echo $REGISTER_DATA | jq -r '.email')
    PASSWORD=$(echo $REGISTER_DATA | jq -r '.password')
    echo "  Token: ${TOKEN:0:50}..."
else
    echo "✗ Registration Failed"
    exit 1
fi
echo ""

# Test 3: User Login
echo "[3/4] Testing User Login..."
LOGIN_DATA=$(cat <<EOF
{
  "email": "$EMAIL",
  "password": "$PASSWORD"
}
EOF
)

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_DATA")

if [ $? -eq 0 ]; then
    echo "✓ Login Successful"
    TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
    USERNAME=$(echo $LOGIN_RESPONSE | jq -r '.username')
    echo "  Username: $USERNAME"
    echo "  Token: ${TOKEN:0:50}..."
else
    echo "✗ Login Failed"
    exit 1
fi
echo ""

# Test 4: Token Validation
echo "[4/4] Testing Token Validation..."
VALIDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/validate" \
    -H "Authorization: Bearer $TOKEN")

if [ $? -eq 0 ]; then
    echo "✓ Token Validation Successful"
    VALID=$(echo $VALIDATE_RESPONSE | jq -r '.valid')
    echo "  Valid: $VALID"
else
    echo "✗ Token Validation Failed"
    exit 1
fi
echo ""

echo "========================================"
echo "  All Tests Passed Successfully! ✓"
echo "========================================"
echo ""
echo "You can now:"
echo "  • Access Swagger UI: $BASE_URL/swagger-ui.html"
echo "  • Access H2 Console: $BASE_URL/h2-console"
echo "  • View API Docs: $BASE_URL/v3/api-docs"
echo ""
