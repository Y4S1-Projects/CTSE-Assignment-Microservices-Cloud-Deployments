# Auth Service API Test Script

# Base URL
$BASE_URL = "http://localhost:8081"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Auth Service API Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[1/4] Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/health" -Method Get
    Write-Host "✓ Health Check: " -NoNewline -ForegroundColor Green
    Write-Host "Service is $($healthResponse.status)" -ForegroundColor White
} catch {
    Write-Host "✗ Health Check Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure the service is running on port 8081" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 2: User Registration
Write-Host "[2/4] Testing User Registration..." -ForegroundColor Yellow
$registerData = @{
    username = "testuser_$(Get-Random -Maximum 9999)"
    email = "testuser_$(Get-Random -Maximum 9999)@example.com"
    password = "test123456"
    fullName = "Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registerData
    
    Write-Host "✓ Registration Successful" -ForegroundColor Green
    Write-Host "  User ID: $($registerResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($registerResponse.username)" -ForegroundColor Gray
    Write-Host "  Token: $($registerResponse.token.Substring(0, 50))..." -ForegroundColor Gray
    
    $global:TOKEN = $registerResponse.token
    $global:EMAIL = ($registerData | ConvertFrom-Json).email
    $global:PASSWORD = ($registerData | ConvertFrom-Json).password
} catch {
    Write-Host "✗ Registration Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 3: User Login
Write-Host "[3/4] Testing User Login..." -ForegroundColor Yellow
$loginData = @{
    email = $global:EMAIL
    password = $global:PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginData
    
    Write-Host "✓ Login Successful" -ForegroundColor Green
    Write-Host "  User ID: $($loginResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($loginResponse.username)" -ForegroundColor Gray
    Write-Host "  Token: $($loginResponse.token.Substring(0, 50))..." -ForegroundColor Gray
    
    $global:TOKEN = $loginResponse.token
} catch {
    Write-Host "✗ Login Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 4: Token Validation
Write-Host "[4/4] Testing Token Validation..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $($global:TOKEN)"
    }
    
    $validateResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/validate" `
        -Method Post `
        -Headers $headers
    
    Write-Host "✓ Token Validation Successful" -ForegroundColor Green
    Write-Host "  Valid: $($validateResponse.valid)" -ForegroundColor Gray
    Write-Host "  User ID: $($validateResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($validateResponse.username)" -ForegroundColor Gray
} catch {
    Write-Host "✗ Token Validation Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  All Tests Passed Successfully! ✓" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now:" -ForegroundColor Yellow
Write-Host "  • Access Swagger UI: $BASE_URL/swagger-ui.html" -ForegroundColor White
Write-Host "  • Access H2 Console: $BASE_URL/h2-console" -ForegroundColor White
Write-Host "  • View API Docs: $BASE_URL/v3/api-docs" -ForegroundColor White
Write-Host ""
