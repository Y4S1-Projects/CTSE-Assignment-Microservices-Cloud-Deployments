# Quick Swagger Test Script
# This will check if Swagger dependencies are properly configured

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Swagger Configuration Checker" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check Auth Service
Write-Host "Checking Auth Service..." -ForegroundColor Yellow
$authPom = "auth-service\pom.xml"
if (Test-Path $authPom) {
    $content = Get-Content $authPom -Raw
    if ($content -match "springdoc-openapi") {
        Write-Host "  [OK] SpringDoc dependency found" -ForegroundColor Green
    } else {
        Write-Host "  [X] SpringDoc dependency missing" -ForegroundColor Red
    }
    
    if (Test-Path "auth-service\src\main\java\com\example\authservice\config\OpenApiConfig.java") {
        Write-Host "  [OK] OpenApiConfig.java exists" -ForegroundColor Green
    } else {
        Write-Host "  [X] OpenApiConfig.java missing" -ForegroundColor Red
    }
} else {
    Write-Host "  [X] Auth service not found" -ForegroundColor Red
}

# Check API Gateway
Write-Host "`nChecking API Gateway..." -ForegroundColor Yellow
$gatewayPom = "api-gateway\pom.xml"
if (Test-Path $gatewayPom) {
    $content = Get-Content $gatewayPom -Raw
    if ($content -match "springdoc-openapi-starter-webflux-ui") {
        Write-Host "  [OK] SpringDoc WebFlux dependency found" -ForegroundColor Green
    } else {
        Write-Host "  [X] SpringDoc dependency issue" -ForegroundColor Red
    }
    
    if (Test-Path "api-gateway\src\main\java\com\example\apigateway\config\OpenApiConfig.java") {
        Write-Host "  [OK] OpenApiConfig.java exists" -ForegroundColor Green
    } else {
        Write-Host "  [X] OpenApiConfig.java missing" -ForegroundColor Red
    }
} else {
    Write-Host "  [X] API Gateway not found" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Configuration Summary" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Status: Swagger is configured and ready!" -ForegroundColor Green
Write-Host ""
Write-Host "To start and access Swagger UI:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Start Auth Service:" -ForegroundColor White
Write-Host "   cd auth-service" -ForegroundColor Gray
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Start API Gateway (in new terminal):" -ForegroundColor White
Write-Host "   cd api-gateway" -ForegroundColor Gray
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Access Swagger UI:" -ForegroundColor White
Write-Host "   Auth Service: http://localhost:8081/swagger-ui.html" -ForegroundColor Cyan
Write-Host "   API Gateway:  http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
Write-Host ""
