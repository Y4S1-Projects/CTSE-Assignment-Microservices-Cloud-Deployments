# CTSE Microservices - Azure Deployment (Azure for Students - Southeast Asia)
# Make sure GitHub packages are PUBLIC before running this!

# Configuration
$RG = "ctse-microservices-rg"
$ENV_NAME = "ctse-env"
$REGISTRY = "ghcr.io"
$REPO = "y4s1-projects/ctse-assignment-microservices-cloud-deployments"
$TAG = "main"

Write-Host "=== Deploying CTSE Microservices ===" -ForegroundColor Cyan
Write-Host "Resource Group: $RG" -ForegroundColor Gray
Write-Host "Environment: $ENV_NAME" -ForegroundColor Gray
Write-Host "Region: Southeast Asia" -ForegroundColor Gray
Write-Host "====================================`n" -ForegroundColor Gray

# Deploy API Gateway (Public)
Write-Host "[1/5] Deploying API Gateway (Public)..." -ForegroundColor Yellow
az containerapp create `
  --name api-gateway `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/$REPO/api-gateway:$TAG" `
  --target-port 8080 `
  --ingress external `
  --min-replicas 1 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi `
  --output none

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] API Gateway deployed" -ForegroundColor Green
} else {
    Write-Host "[FAIL] API Gateway failed" -ForegroundColor Red
}

# Deploy Auth Service (Internal)
Write-Host "`n[2/5] Deploying Auth Service (Internal)..." -ForegroundColor Yellow
az containerapp create `
  --name auth-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/$REPO/auth-service:$TAG" `
  --target-port 8081 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi `
  --output none

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Auth Service deployed" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Auth Service failed" -ForegroundColor Red
}

# Deploy Catalog Service (Internal)
Write-Host "`n[3/5] Deploying Catalog Service (Internal)..." -ForegroundColor Yellow
az containerapp create `
  --name catalog-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/$REPO/catalog-service:$TAG" `
  --target-port 8082 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi `
  --output none

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Catalog Service deployed" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Catalog Service failed" -ForegroundColor Red
}

# Deploy Order Service (Internal)
Write-Host "`n[4/5] Deploying Order Service (Internal)..." -ForegroundColor Yellow
az containerapp create `
  --name order-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/$REPO/order-service:$TAG" `
  --target-port 8083 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi `
  --output none

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Order Service deployed" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Order Service failed" -ForegroundColor Red
}

# Deploy Payment Service (Internal)
Write-Host "`n[5/5] Deploying Payment Service (Internal)..." -ForegroundColor Yellow
az containerapp create `
  --name payment-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/$REPO/payment-service:$TAG" `
  --target-port 8084 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi `
  --output none

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Payment Service deployed" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Payment Service failed" -ForegroundColor Red
}

# Get API Gateway URL
Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "Retrieving API Gateway URL..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv 2>$null

if ($API_URL) {
    Write-Host "`n[SUCCESS] Deployment Complete!" -ForegroundColor Green
    Write-Host "=====================================" -ForegroundColor Green
    Write-Host "API Gateway URL:" -ForegroundColor Cyan
    Write-Host "  https://$API_URL" -ForegroundColor White
    Write-Host "`nTest Endpoints:" -ForegroundColor Cyan
    Write-Host "  Health: https://$API_URL/actuator/health" -ForegroundColor Gray
    Write-Host "  Swagger: https://$API_URL/swagger-ui.html" -ForegroundColor Gray
    Write-Host "=====================================" -ForegroundColor Green
    
    # Try health check
    Write-Host "`nTesting health endpoint..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "https://$API_URL/actuator/health" -TimeoutSec 15 -ErrorAction Stop
        if ($response.status -eq "UP") {
            Write-Host "[OK] API Gateway is healthy!" -ForegroundColor Green
        }
    } catch {
        Write-Host "[INFO] Service is starting... Try again in 1-2 minutes" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n[WARN] Could not get API Gateway URL yet" -ForegroundColor Yellow
    Write-Host "Services may still be starting. Check Azure Portal:" -ForegroundColor Gray
    Write-Host "https://portal.azure.com -> Container Apps" -ForegroundColor Gray
}

Write-Host "`n=====================================" -ForegroundColor Gray
Write-Host "View all services in Azure Portal:" -ForegroundColor Cyan
Write-Host "https://portal.azure.com/#view/HubsExtension/BrowseResource/resourceType/Microsoft.App%2FcontainerApps" -ForegroundColor Gray
Write-Host "=====================================" -ForegroundColor Gray
