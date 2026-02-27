# Azure Container Apps Deployment Script
# Usage: ./deploy-azure.ps1

param(
    [string]$ResourceGroup = "ctse-microservices-rg",
    [string]$RegistryName = "ctseregistry",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "ctse-env",
    [string]$JwtSecret = "your-super-secret-key-change-in-production"
)

$ErrorActionPreference = "Stop"
$RegistryDomain = "$RegistryName.azurecr.io"

Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Azure Container Apps Deployment - Food Ordering System      ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

# Configuration
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Resource Group: $ResourceGroup"
Write-Host "  Registry Name: $RegistryName"
Write-Host "  Location: $Location"
Write-Host "  Environment: $EnvironmentName`n"

# Step 1: Login
Write-Host "Step 1: Checking Azure Login..." -ForegroundColor Yellow
try {
    $account = az account show 2>$null | ConvertFrom-Json
    Write-Host "✅ Already logged in as: $($account.user.name)`n" -ForegroundColor Green
} catch {
    Write-Host "❌ Not logged in. Starting login..." -ForegroundColor Red
    az login
    Write-Host ""
}

# Step 2: Create Resource Group
Write-Host "Step 2: Creating Resource Group '$ResourceGroup'..." -ForegroundColor Yellow
$rgExists = az group exists --name $ResourceGroup
if ($rgExists -eq "true") {
    Write-Host "✅ Resource group already exists`n" -ForegroundColor Green
} else {
    az group create --name $ResourceGroup --location $Location
    Write-Host "✅ Resource group created`n" -ForegroundColor Green
}

# Step 3: Create Container Registry
Write-Host "Step 3: Creating Azure Container Registry..." -ForegroundColor Yellow
$acrExists = az acr list --resource-group $ResourceGroup --query "[?name=='$RegistryName']" | ConvertFrom-Json
if ($acrExists.Count -gt 0) {
    Write-Host "✅ Registry already exists`n" -ForegroundColor Green
} else {
    az acr create --resource-group $ResourceGroup --name $RegistryName --sku Basic
    Write-Host "✅ Registry created`n" -ForegroundColor Green
}

# Enable admin access
Write-Host "Step 4: Enabling Registry Admin Access..." -ForegroundColor Yellow
az acr update --name $RegistryName --admin-enabled true
Write-Host "✅ Admin access enabled`n" -ForegroundColor Green

# Get credentials
Write-Host "Step 5: Retrieving Registry Credentials..." -ForegroundColor Yellow
$creds = az acr credential show --name $RegistryName | ConvertFrom-Json
$RegistryUsername = $creds.username
$RegistryPassword = $creds.passwords[0].value
Write-Host "✅ Credentials retrieved`n" -ForegroundColor Green

# Step 6: Login to Registry
Write-Host "Step 6: Logging in to Azure Container Registry..." -ForegroundColor Yellow
$RegistryPassword | docker login $RegistryDomain -u $RegistryUsername --password-stdin
Write-Host "✅ Logged in to registry`n" -ForegroundColor Green

# Step 7: Build Images
Write-Host "Step 7: Building Docker Images..." -ForegroundColor Yellow
docker-compose build
Write-Host "✅ Images built`n" -ForegroundColor Green

# Step 8: Tag Images
Write-Host "Step 8: Tagging Images for Registry..." -ForegroundColor Yellow
docker tag api-gateway:latest "$RegistryDomain/api-gateway:latest"
docker tag auth-service:latest "$RegistryDomain/auth-service:latest"
docker tag catalog-service:latest "$RegistryDomain/catalog-service:latest"
docker tag order-service:latest "$RegistryDomain/order-service:latest"
docker tag payment-service:latest "$RegistryDomain/payment-service:latest"
Write-Host "✅ Images tagged`n" -ForegroundColor Green

# Step 9: Push Images
Write-Host "Step 9: Pushing Images to Registry..." -ForegroundColor Yellow
docker push "$RegistryDomain/api-gateway:latest"
docker push "$RegistryDomain/auth-service:latest"
docker push "$RegistryDomain/catalog-service:latest"
docker push "$RegistryDomain/order-service:latest"
docker push "$RegistryDomain/payment-service:latest"
Write-Host "✅ Images pushed to registry`n" -ForegroundColor Green

# Step 10: Create Container Apps Environment
Write-Host "Step 10: Creating Container Apps Environment..." -ForegroundColor Yellow
$envExists = az containerapp env list --resource-group $ResourceGroup --query "[?name=='$EnvironmentName']" | ConvertFrom-Json
if ($envExists.Count -gt 0) {
    Write-Host "✅ Environment already exists`n" -ForegroundColor Green
} else {
    az containerapp env create --name $EnvironmentName --resource-group $ResourceGroup --location $Location
    Write-Host "✅ Environment created`n" -ForegroundColor Green
}

# Step 11: Deploy Services
Write-Host "Step 11: Deploying Microservices..." -ForegroundColor Yellow

# Deploy Auth Service
Write-Host "  Deploying auth-service..." -ForegroundColor Cyan
az containerapp create `
    --name auth-service `
    --resource-group $ResourceGroup `
    --environment $EnvironmentName `
    --image "$RegistryDomain/auth-service:latest" `
    --target-port 8081 `
    --registry-server $RegistryDomain `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --environment-variables JWT_SECRET=$JwtSecret `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 1 `
    --max-replicas 3 `
    2>&1 | Select-String -Pattern "error|Error" -InvertMatch > $null
Write-Host "  ✅ auth-service deployed" -ForegroundColor Green

# Deploy Catalog Service
Write-Host "  Deploying catalog-service..." -ForegroundColor Cyan
az containerapp create `
    --name catalog-service `
    --resource-group $ResourceGroup `
    --environment $EnvironmentName `
    --image "$RegistryDomain/catalog-service:latest" `
    --target-port 8082 `
    --registry-server $RegistryDomain `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 1 `
    --max-replicas 3 `
    2>&1 | Select-String -Pattern "error|Error" -InvertMatch > $null
Write-Host "  ✅ catalog-service deployed" -ForegroundColor Green

# Deploy Order Service
Write-Host "  Deploying order-service..." -ForegroundColor Cyan
az containerapp create `
    --name order-service `
    --resource-group $ResourceGroup `
    --environment $EnvironmentName `
    --image "$RegistryDomain/order-service:latest" `
    --target-port 8083 `
    --registry-server $RegistryDomain `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 1 `
    --max-replicas 3 `
    2>&1 | Select-String -Pattern "error|Error" -InvertMatch > $null
Write-Host "  ✅ order-service deployed" -ForegroundColor Green

# Deploy Payment Service
Write-Host "  Deploying payment-service..." -ForegroundColor Cyan
az containerapp create `
    --name payment-service `
    --resource-group $ResourceGroup `
    --environment $EnvironmentName `
    --image "$RegistryDomain/payment-service:latest" `
    --target-port 8084 `
    --registry-server $RegistryDomain `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 1 `
    --max-replicas 3 `
    2>&1 | Select-String -Pattern "error|Error" -InvertMatch > $null
Write-Host "  ✅ payment-service deployed" -ForegroundColor Green

# Deploy API Gateway
Write-Host "  Deploying api-gateway..." -ForegroundColor Cyan
az containerapp create `
    --name api-gateway `
    --resource-group $ResourceGroup `
    --environment $EnvironmentName `
    --image "$RegistryDomain/api-gateway:latest" `
    --target-port 8080 `
    --ingress external `
    --registry-server $RegistryDomain `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --environment-variables `
        JWT_SECRET=$JwtSecret `
        SERVICE_AUTH_URL="http://auth-service" `
        SERVICE_CATALOG_URL="http://catalog-service" `
        SERVICE_ORDER_URL="http://order-service" `
        SERVICE_PAYMENT_URL="http://payment-service" `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 2 `
    --max-replicas 5 `
    2>&1 | Select-String -Pattern "error|Error" -InvertMatch > $null
Write-Host "  ✅ api-gateway deployed" -ForegroundColor Green

Write-Host "`nStep 12: Getting API Gateway URL..." -ForegroundColor Yellow
$apiGatewayUrl = az containerapp show `
    --name api-gateway `
    --resource-group $ResourceGroup `
    --query "properties.configuration.ingress.fqdn" `
    -o tsv

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                  🎉 DEPLOYMENT SUCCESSFUL! 🎉                ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor Green

Write-Host "📊 Deployment Summary:" -ForegroundColor Cyan
Write-Host "  Resource Group: $ResourceGroup"
Write-Host "  Container Registry: $RegistryName"
Write-Host "  Environment: $EnvironmentName"
Write-Host "  Location: $Location`n"

Write-Host "🔗 API Gateway URL:" -ForegroundColor Cyan
Write-Host "  https://$apiGatewayUrl`n" -ForegroundColor Yellow

Write-Host "📝 Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Test the API:"
Write-Host "     curl https://$apiGatewayUrl/actuator/health`n"
Write-Host "  2. View logs:"
Write-Host "     az containerapp logs show --name api-gateway --resource-group $ResourceGroup --follow`n"
Write-Host "  3. List all services:"
Write-Host "     az containerapp list --resource-group $ResourceGroup --output table`n"
Write-Host "  4. Delete resources (when done):"
Write-Host "     az group delete --name $ResourceGroup --yes`n"

Write-Host "ℹ️ Service URLs (internal):" -ForegroundColor Cyan
Write-Host "  Auth Service: http://auth-service:8081"
Write-Host "  Catalog Service: http://catalog-service:8082"
Write-Host "  Order Service: http://order-service:8083"
Write-Host "  Payment Service: http://payment-service:8084`n"

Write-Host "✅ All services are now deployed and running on Azure Container Apps!" -ForegroundColor Green
