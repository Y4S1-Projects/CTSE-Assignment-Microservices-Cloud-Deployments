# ⚡ API Gateway - Quick Start Guide

> Get the API Gateway up and running in 5 minutes!

---

## 🚀 Quick Commands

### 1. Build the Project

```bash
cd api-gateway
mvn clean package -DskipTests
```

### 2. Run the Gateway

```bash
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### 3. Verify It's Running

```bash
curl http://localhost:8080/health
```

**Expected:**

```json
{
	"status": "UP",
	"service": "api-gateway",
	"timestamp": "2026-02-26T22:30:00",
	"port": 8080
}
```

---

## 📋 Prerequisites Checklist

- [x] Java 17+ installed (`java -version`)
- [x] Maven 3.9+ installed (`mvn -version`)
- [x] Port 8080 available
- [x] Auth Service running on port 8081 (for testing)

---

## 🔧 Configuration

### Set JWT Secret (Important!)

**Windows PowerShell:**

```powershell
$env:JWT_SECRET="your-super-secret-key-change-in-production-env"
```

**Linux/Mac:**

```bash
export JWT_SECRET="your-super-secret-key-change-in-production-env"
```

### Service URLs (Docker Environment)

The gateway expects these services to be available:

- Auth Service: `http://auth-service:8081`
- Catalog Service: `http://catalog-service:8082`
- Order Service: `http://order-service:8083`
- Payment Service: `http://payment-service:8084`

**For local testing**, update `application.properties`:

```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8081  # Auth Service
spring.cloud.gateway.routes[1].uri=http://localhost:8082  # Catalog Service
spring.cloud.gateway.routes[2].uri=http://localhost:8083  # Order Service
spring.cloud.gateway.routes[3].uri=http://localhost:8084  # Payment Service
```

---

## 🧪 Quick Test

### Test 1: Health Check

```bash
curl http://localhost:8080/health
```

### Test 2: Get Gateway Info

```bash
curl http://localhost:8080/
```

### Test 3: Register User (via Gateway)

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"testuser\",
    \"email\": \"test@example.com\",
    \"password\": \"password123\",
    \"fullName\": \"Test User\"
  }"
```

**Expected:**

```json
{
	"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
	"username": "testuser",
	"userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Test 4: Login (via Gateway)

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test@example.com\",
    \"password\": \"password123\"
  }"
```

### Test 5: Access Protected Route

```bash
# Save token from login response
TOKEN="<your-jwt-token>"

# Access catalog (when catalog service is running)
curl -X GET http://localhost:8080/catalog/items \
  -H "Authorization: Bearer $TOKEN"
```

### Test 6: Test Rate Limiting

```bash
# Send 105 requests rapidly (limit is 100/min)
for i in {1..105}; do
  curl -s http://localhost:8080/health | grep -q "UP" && echo "✓ Request $i" || echo "✗ Request $i FAILED"
done
```

After 100 requests, you should see rate limit errors.

---

## 🐳 Docker Quick Start

### Build Image

```bash
docker build -t api-gateway:latest .
```

### Run Container

```bash
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e JWT_SECRET="your-production-secret-key" \
  api-gateway:latest
```

### View Logs

```bash
docker logs -f api-gateway
```

### Stop Container

```bash
docker stop api-gateway
docker rm api-gateway
```

---

## 🔍 Testing with PowerShell Script

Create `test-gateway.ps1`:

```powershell
$BASE_URL = "http://localhost:8080"

Write-Host "🧪 Testing API Gateway..." -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "1️⃣  Testing Health Check..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "$BASE_URL/health" -Method Get
if ($response.status -eq "UP") {
    Write-Host "✅ Health check passed" -ForegroundColor Green
} else {
    Write-Host "❌ Health check failed" -ForegroundColor Red
}
Write-Host ""

# Test 2: Gateway Info
Write-Host "2️⃣  Testing Gateway Info..." -ForegroundColor Yellow
$info = Invoke-RestMethod -Uri "$BASE_URL/" -Method Get
Write-Host "✅ Gateway: $($info.service) - $($info.status)" -ForegroundColor Green
Write-Host "   Available Routes:" -ForegroundColor Cyan
$info.routes.PSObject.Properties | ForEach-Object {
    Write-Host "   - $($_.Name): $($_.Value)" -ForegroundColor Gray
}
Write-Host ""

# Test 3: Register User
Write-Host "3️⃣  Testing User Registration (via Gateway)..." -ForegroundColor Yellow
$registerBody = @{
    username = "gatewaytest_$(Get-Random -Maximum 9999)"
    email = "gatewaytest_$(Get-Random -Maximum 9999)@example.com"
    password = "password123"
    fullName = "Gateway Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registerBody

    Write-Host "✅ Registration successful" -ForegroundColor Green
    Write-Host "   Username: $($registerResponse.username)" -ForegroundColor Gray
    Write-Host "   User ID: $($registerResponse.userId)" -ForegroundColor Gray
    $token = $registerResponse.token
    Write-Host ""

    # Test 4: Validate Token
    Write-Host "4️⃣  Testing JWT Validation..." -ForegroundColor Yellow
    $validateBody = @{ token = $token } | ConvertTo-Json
    $validateResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/validate" `
        -Method Post `
        -ContentType "application/json" `
        -Body $validateBody

    Write-Host "✅ Token validation successful" -ForegroundColor Green
    Write-Host "   User: $($validateResponse.username)" -ForegroundColor Gray
    Write-Host ""

    # Test 5: Rate Limiting
    Write-Host "5️⃣  Testing Rate Limiting (10 rapid requests)..." -ForegroundColor Yellow
    $successCount = 0
    $rateLimitCount = 0

    for ($i = 1; $i -le 10; $i++) {
        try {
            Invoke-RestMethod -Uri "$BASE_URL/health" -Method Get -ErrorAction Stop | Out-Null
            $successCount++
        } catch {
            if ($_.Exception.Response.StatusCode -eq 429) {
                $rateLimitCount++
            }
        }
    }

    Write-Host "   Successful requests: $successCount" -ForegroundColor Green
    if ($rateLimitCount -gt 0) {
        Write-Host "   Rate limited: $rateLimitCount" -ForegroundColor Yellow
    }
    Write-Host ""

} catch {
    Write-Host "❌ Test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "🎉 Gateway testing complete!" -ForegroundColor Cyan
```

**Run the test:**

```powershell
.\test-gateway.ps1
```

---

## 🔍 Testing with Bash Script

Create `test-gateway.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "🧪 Testing API Gateway..."
echo ""

# Test 1: Health Check
echo "1️⃣  Testing Health Check..."
response=$(curl -s $BASE_URL/health)
status=$(echo $response | jq -r '.status')
if [ "$status" == "UP" ]; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed"
fi
echo ""

# Test 2: Gateway Info
echo "2️⃣  Testing Gateway Info..."
info=$(curl -s $BASE_URL/)
service=$(echo $info | jq -r '.service')
echo "✅ Gateway: $service - running"
echo "   Available Routes:"
echo $info | jq -r '.routes | to_entries[] | "   - \(.key): \(.value)"'
echo ""

# Test 3: Register User
echo "3️⃣  Testing User Registration (via Gateway)..."
username="gatewaytest_$RANDOM"
email="gatewaytest_$RANDOM@example.com"

register_response=$(curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$username\",
    \"email\": \"$email\",
    \"password\": \"password123\",
    \"fullName\": \"Gateway Test User\"
  }")

token=$(echo $register_response | jq -r '.token')
user_id=$(echo $register_response | jq -r '.userId')

if [ "$token" != "null" ] && [ ! -z "$token" ]; then
    echo "✅ Registration successful"
    echo "   Username: $username"
    echo "   User ID: $user_id"
    echo ""

    # Test 4: Validate Token
    echo "4️⃣  Testing JWT Validation..."
    validate_response=$(curl -s -X POST $BASE_URL/auth/validate \
      -H "Content-Type: application/json" \
      -d "{\"token\": \"$token\"}")

    valid_user=$(echo $validate_response | jq -r '.username')
    if [ "$valid_user" == "$username" ]; then
        echo "✅ Token validation successful"
        echo "   User: $valid_user"
    else
        echo "❌ Token validation failed"
    fi
    echo ""

    # Test 5: Rate Limiting
    echo "5️⃣  Testing Rate Limiting (10 rapid requests)..."
    success_count=0
    rate_limit_count=0

    for i in {1..10}; do
        status_code=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/health)
        if [ "$status_code" == "200" ]; then
            ((success_count++))
        elif [ "$status_code" == "429" ]; then
            ((rate_limit_count++))
        fi
    done

    echo "   Successful requests: $success_count"
    if [ $rate_limit_count -gt 0 ]; then
        echo "   Rate limited: $rate_limit_count"
    fi
    echo ""
else
    echo "❌ Registration failed"
fi

echo "🎉 Gateway testing complete!"
```

**Run the test:**

```bash
chmod +x test-gateway.sh
./test-gateway.sh
```

---

## 🐞 Common Issues

### Issue 1: Port 8080 Already in Use

**Windows:**

```powershell
# Find process
netstat -ano | findstr :8080

# Kill process
taskkill /PID <pid> /F
```

**Linux/Mac:**

```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <pid>
```

### Issue 2: Cannot Connect to Services

**Check Service URLs:**

```bash
# Test auth service
curl http://localhost:8081/auth/health

# If using Docker, use container names
curl http://auth-service:8081/auth/health
```

**Update application.properties** for local testing:

```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8081
```

### Issue 3: JWT_SECRET Mismatch

Ensure the same secret is used in both Auth Service and API Gateway:

```bash
# Windows PowerShell
$env:JWT_SECRET="your-super-secret-key-change-in-production-env"

# Linux/Mac
export JWT_SECRET="your-super-secret-key-change-in-production-env"
```

### Issue 4: CORS Errors

If testing from a web browser, CORS is already configured. For production:

Edit `CorsConfig.java`:

```java
corsConfig.setAllowedOrigins(Arrays.asList(
    "http://localhost:3000",
    "https://yourdomain.com"
));
```

---

## 📊 View Logs

### Console Logs

```bash
# Maven run (logs to console)
mvn spring-boot:run

# JAR run (logs to console)
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

### Docker Logs

```bash
# Follow logs
docker logs -f api-gateway

# Last 100 lines
docker logs --tail 100 api-gateway
```

---

## 🔗 Swagger UI

Once running, access API documentation:

**URL:** http://localhost:8080/swagger-ui.html

---

## 📞 Need Help?

See full documentation: [README.md](README.md)

---

**Last Updated:** February 26, 2026  
**Version:** 1.0.0
