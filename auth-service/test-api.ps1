# Auth Service API Test Script

param(
    [string]$BaseUrl = "http://localhost:8081"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Auth Service API Testing Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[1/4] Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/health" -Method Get
    Write-Host "[OK] Health Check: " -NoNewline -ForegroundColor Green
    Write-Host "Service is $($healthResponse.status)" -ForegroundColor White
} catch {
    Write-Host "[FAIL] Health Check Failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure the service is running on the configured URL: $BaseUrl" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 2: User Registration
Write-Host "[2/4] Testing User Registration..." -ForegroundColor Yellow
$registerData = @{
    username = "testuser_$(Get-Random -Maximum 9999)"
    email = "testuser_$(Get-Random -Maximum 9999)@example.com"
    password = "Password1!"
    fullName = "Test User"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" `
        -Method Post `
        -ContentType "application/json" `
        -Body $registerData

    $registeredToken = if ($registerResponse.accessToken) { $registerResponse.accessToken } else { $registerResponse.token }
    
    Write-Host "[OK] Registration Successful" -ForegroundColor Green
    Write-Host "  User ID: $($registerResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($registerResponse.username)" -ForegroundColor Gray
    Write-Host "  Token: $($registeredToken.Substring(0, 50))..." -ForegroundColor Gray
    
    $global:TOKEN = $registeredToken
    $global:EMAIL = ($registerData | ConvertFrom-Json).email
    $global:PASSWORD = ($registerData | ConvertFrom-Json).password
} catch {
    Write-Host "[FAIL] Registration Failed: $($_.Exception.Message)" -ForegroundColor Red
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
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginData

    $loginToken = if ($loginResponse.accessToken) { $loginResponse.accessToken } else { $loginResponse.token }
    
    Write-Host "[OK] Login Successful" -ForegroundColor Green
    Write-Host "  User ID: $($loginResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($loginResponse.username)" -ForegroundColor Gray
    Write-Host "  Token: $($loginToken.Substring(0, 50))..." -ForegroundColor Gray
    
    $global:TOKEN = $loginToken
} catch {
    Write-Host "[FAIL] Login Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test 4: Token Validation
Write-Host "[4/4] Testing Token Validation..." -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer $($global:TOKEN)"
    }
    
    $validateResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/validate" `
        -Method Post `
        -Headers $headers
    
    Write-Host "[OK] Token Validation Successful" -ForegroundColor Green
    Write-Host "  Valid: $($validateResponse.valid)" -ForegroundColor Gray
    Write-Host "  User ID: $($validateResponse.userId)" -ForegroundColor Gray
    Write-Host "  Username: $($validateResponse.username)" -ForegroundColor Gray
} catch {
    Write-Host "[FAIL] Token Validation Failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  All Tests Passed Successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now:" -ForegroundColor Yellow
Write-Host "  - Access Swagger UI: $BaseUrl/auth/swagger-ui.html" -ForegroundColor White
Write-Host "  - View API Docs: $BaseUrl/auth/v3/api-docs" -ForegroundColor White
Write-Host ""
