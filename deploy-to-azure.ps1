# CTSE Microservices - Azure Deployment Script
# For Azure for Students accounts that cannot create service principals

# Configuration
$RG = "ctse-microservices-rg"
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$REGISTRY = "ghcr.io"
$REPO = "y4s1-projects/ctse-assignment-microservices-cloud-deployments"
$TAG = "main"

Write-Host "=== Deploying CTSE Microservices to Azure Container Apps ===" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Gray

# Verify Azure CLI login
Write-Host "`nChecking Azure authentication..." -ForegroundColor Yellow
$account = az account show 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Not logged into Azure CLI!" -ForegroundColor Red
    Write-Host "Please run: az login" -ForegroundColor Yellow
    exit 1
}
Write-Host "[OK] Logged in as: $(az account show --query user.name -o tsv)" -ForegroundColor Green

# Create Resource Group
Write-Host "`n[STEP 1/4] Creating resource group '$RG' in $LOCATION..." -ForegroundColor Yellow
az group create --name $RG --location $LOCATION --output none
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Resource group ready" -ForegroundColor Green
} else {
    Write-Host "[WARN] Resource group creation failed, it might already exist" -ForegroundColor Yellow
}

# Create Container Apps Environment
Write-Host "`n[STEP 2/4] Creating Container Apps environment '$ENV_NAME'..." -ForegroundColor Yellow
az containerapp env create `
  --name $ENV_NAME `
  --resource-group $RG `
  --location $LOCATION `
  --output none
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Environment created successfully" -ForegroundColor Green
} else {
    Write-Host "[WARN] Environment creation failed, it might already exist" -ForegroundColor Yellow
}

# Wait for environment to be ready
Write-Host "`n[STEP 3/4] Waiting for environment to be ready (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Deploy Services
Write-Host "`n[STEP 4/4] Deploying microservices..." -ForegroundColor Cyan

$services = @(
    @{name="api-gateway"; port=8080; ingress="external"; min=1; description="API Gateway (Public)"},
    @{name="auth-service"; port=8081; ingress="internal"; min=0; description="Auth Service (Private)"},
    @{name="catalog-service"; port=8082; ingress="internal"; min=0; description="Catalog Service (Private)"},
    @{name="order-service"; port=8083; ingress="internal"; min=0; description="Order Service (Private)"},
    @{name="payment-service"; port=8084; ingress="internal"; min=0; description="Payment Service (Private)"}
)

$successCount = 0
$failCount = 0

foreach ($svc in $services) {
    Write-Host "`n  [>>] Deploying $($svc.description)..." -ForegroundColor Yellow
    Write-Host "       Image: $REGISTRY/$REPO/$($svc.name):$TAG" -ForegroundColor Gray
    
    az containerapp create `
      --name $svc.name `
      --resource-group $RG `
      --environment $ENV_NAME `
      --image "$REGISTRY/$REPO/$($svc.name):$TAG" `
      --target-port $svc.port `
      --ingress $svc.ingress `
      --min-replicas $svc.min `
      --max-replicas 2 `
      --cpu 0.5 `
      --memory 1Gi `
      --output none
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "       [OK] $($svc.name) deployed successfully" -ForegroundColor Green
        $successCount++
    } else {
        Write-Host "       [FAIL] $($svc.name) deployment failed" -ForegroundColor Red
        $failCount++
    }
}

# Summary
Write-Host "`n============================================================" -ForegroundColor Gray
Write-Host "Deployment Summary:" -ForegroundColor Cyan
Write-Host "   [OK] Successful: $successCount services" -ForegroundColor Green
if ($failCount -gt 0) {
    Write-Host "   [FAIL] Failed: $failCount services" -ForegroundColor Red
}

# Get API Gateway URL
Write-Host "`nRetrieving API Gateway URL..." -ForegroundColor Yellow
Start-Sleep -Seconds 5  # Give Azure a moment to register the URL

$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv 2>$null

if ($API_URL) {
    Write-Host "`n[SUCCESS] Deployment Complete!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "API Gateway URL:" -ForegroundColor Cyan
    Write-Host "   https://$API_URL" -ForegroundColor White
    Write-Host "`nEndpoints:" -ForegroundColor Cyan
    Write-Host "   Health Check:  https://$API_URL/actuator/health" -ForegroundColor Gray
    Write-Host "   Swagger UI:    https://$API_URL/swagger-ui.html" -ForegroundColor Gray
    Write-Host "   API Docs:      https://$API_URL/v3/api-docs" -ForegroundColor Gray
    Write-Host "============================================================" -ForegroundColor Green
    
    # Test health endpoint
    Write-Host "`nTesting health endpoint..." -ForegroundColor Yellow
    try {
        $response = Invoke-RestMethod -Uri "https://$API_URL/actuator/health" -TimeoutSec 10 -ErrorAction Stop
        if ($response.status -eq "UP") {
            Write-Host "[OK] API Gateway is healthy!" -ForegroundColor Green
        }
    } catch {
        Write-Host "[WARN] Health check pending (service may still be starting up)" -ForegroundColor Yellow
        Write-Host "       Wait a few minutes and try: curl https://$API_URL/actuator/health" -ForegroundColor Gray
    }
} else {
    Write-Host "`n[WARN] Could not retrieve API Gateway URL" -ForegroundColor Yellow
    Write-Host "       The deployment may still be in progress" -ForegroundColor Gray
}

# Azure Portal link
Write-Host "`nView in Azure Portal:" -ForegroundColor Cyan
Write-Host "   https://portal.azure.com/#view/HubsExtension/BrowseResource/resourceType/Microsoft.App%2FcontainerApps" -ForegroundColor Gray

# Next steps
Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "   1. Check Azure Portal for container status" -ForegroundColor Gray
Write-Host "   2. Wait 2-3 minutes for services to start" -ForegroundColor Gray
Write-Host "   3. Test the API Gateway health endpoint" -ForegroundColor Gray
Write-Host "   4. View logs: az containerapp logs show --name api-gateway --resource-group $RG --type console" -ForegroundColor Gray

Write-Host "`nTo update services after code changes:" -ForegroundColor Yellow
Write-Host "   1. Push to GitHub (triggers build & publish)" -ForegroundColor Gray
Write-Host "   2. Run: .\deploy-to-azure.ps1" -ForegroundColor Gray

Write-Host "`n============================================================`n" -ForegroundColor Gray
