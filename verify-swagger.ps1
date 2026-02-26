# Swagger Verification Script for All Services
# Tests Swagger UI and OpenAPI endpoints

$ErrorActionPreference = "Continue"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Swagger UI Verification Script" -ForegroundColor Cyan
Write-Host "  Food Ordering Microservices" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Define services
$services = @(
    @{
        Name = "Auth Service"
        Port = 8081
        SwaggerUI = "http://localhost:8081/swagger-ui.html"
        ApiDocs = "http://localhost:8081/v3/api-docs"
        Health = "http://localhost:8081/auth/health"
    },
    @{
        Name = "API Gateway"
        Port = 8080
        SwaggerUI = "http://localhost:8080/swagger-ui.html"
        ApiDocs = "http://localhost:8080/v3/api-docs"
        Health = "http://localhost:8080/health"
    }
)

$allPassed = $true

foreach ($service in $services) {
    Write-Host "`n┌────────────────────────────────────────┐" -ForegroundColor White
    Write-Host "│  Testing: $($service.Name.PadRight(28)) │" -ForegroundColor White
    Write-Host "└────────────────────────────────────────┘" -ForegroundColor White
    
    # Test 1: Service Health Check
    Write-Host "`n1️⃣  Health Check..." -ForegroundColor Yellow
    try {
        $healthResponse = Invoke-RestMethod -Uri $service.Health -Method Get -TimeoutSec 5 -ErrorAction Stop
        Write-Host "   ✅ Service is running" -ForegroundColor Green
        Write-Host "   Status: $($healthResponse.status)" -ForegroundColor Gray
    } catch {
        Write-Host "   ❌ Service not responding" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
        Write-Host "   Make sure the service is running on port $($service.Port)" -ForegroundColor Yellow
        $allPassed = $false
        continue
    }
    
    # Test 2: OpenAPI JSON Docs
    Write-Host "`n2️⃣  OpenAPI JSON Docs..." -ForegroundColor Yellow
    try {
        $apiDocs = Invoke-RestMethod -Uri $service.ApiDocs -Method Get -TimeoutSec 5 -ErrorAction Stop
        Write-Host "   ✅ OpenAPI docs accessible" -ForegroundColor Green
        Write-Host "   URL: $($service.ApiDocs)" -ForegroundColor Gray
        
        # Check OpenAPI version
        if ($apiDocs.openapi) {
            Write-Host "   OpenAPI Version: $($apiDocs.openapi)" -ForegroundColor Gray
        }
        
        # Count endpoints
        if ($apiDocs.paths) {
            $pathCount = ($apiDocs.paths.PSObject.Properties | Measure-Object).Count
            Write-Host "   Documented Paths: $pathCount" -ForegroundColor Gray
        }
    } catch {
        Write-Host "   ❌ OpenAPI docs not accessible" -ForegroundColor Red
        Write-Host "   URL: $($service.ApiDocs)" -ForegroundColor Gray
        $allPassed = $false
    }
    
    # Test 3: Swagger UI
    Write-Host "`n3️⃣  Swagger UI..." -ForegroundColor Yellow
    try {
        $swaggerResponse = Invoke-WebRequest -Uri $service.SwaggerUI -Method Get -TimeoutSec 5 -ErrorAction Stop
        
        if ($swaggerResponse.StatusCode -eq 200) {
            Write-Host "   ✅ Swagger UI accessible" -ForegroundColor Green
            Write-Host "   URL: $($service.SwaggerUI)" -ForegroundColor Gray
            
            # Check if it's actually Swagger UI
            if ($swaggerResponse.Content -match "swagger-ui") {
                Write-Host "   ✅ Swagger UI content verified" -ForegroundColor Green
            } else {
                Write-Host "   ⚠️  Response received but may not be Swagger UI" -ForegroundColor Yellow
            }
        } else {
            Write-Host "   ❌ Unexpected status code: $($swaggerResponse.StatusCode)" -ForegroundColor Red
            $allPassed = $false
        }
    } catch {
        Write-Host "   ❌ Swagger UI not accessible" -ForegroundColor Red
        Write-Host "   URL: $($service.SwaggerUI)" -ForegroundColor Gray
        $allPassed = $false
    }
    
    # Test 4: Browser Instructions
    Write-Host "`n4️⃣  Browser Access..." -ForegroundColor Yellow
    Write-Host "   Open in browser: $($service.SwaggerUI)" -ForegroundColor Cyan
    Write-Host "   Press Ctrl+Click to open (in VS Code)" -ForegroundColor Gray
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Test Summary" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

if ($allPassed) {
    Write-Host "🎉 All services passed Swagger verification!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📌 Quick Access:" -ForegroundColor Cyan
    Write-Host "   Auth Service:  http://localhost:8081/swagger-ui.html" -ForegroundColor White
    Write-Host "   API Gateway:   http://localhost:8080/swagger-ui.html" -ForegroundColor White
    Write-Host ""
    Write-Host "💡 Next Steps:" -ForegroundColor Yellow
    Write-Host "   1. Open Swagger UI in your browser" -ForegroundColor Gray
    Write-Host "   2. Try the 'POST /auth/register' endpoint" -ForegroundColor Gray
    Write-Host "   3. Login with 'POST /auth/login' to get JWT token" -ForegroundColor Gray
    Write-Host "   4. Click 'Authorize' and enter: Bearer <your-token>" -ForegroundColor Gray
    Write-Host "   5. Test protected endpoints" -ForegroundColor Gray
} else {
    Write-Host "⚠️  Some services failed verification" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "🔧 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   1. Check if services are running:" -ForegroundColor Gray
    Write-Host "      cd auth-service && mvn spring-boot:run" -ForegroundColor Gray
    Write-Host "      cd api-gateway && mvn spring-boot:run" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   2. Verify port availability:" -ForegroundColor Gray
    Write-Host "      netstat -ano | findstr :8081" -ForegroundColor Gray
    Write-Host "      netstat -ano | findstr :8080" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   3. Check logs for errors" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   4. Rebuild services:" -ForegroundColor Gray
    Write-Host "      mvn clean package -DskipTests" -ForegroundColor Gray
}

Write-Host ""
Write-Host "📚 Documentation: SWAGGER_API_DOCUMENTATION.md" -ForegroundColor Cyan
Write-Host ""
