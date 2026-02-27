# Azure for Students Deployment Guide

## The Problem

Your Azure for Students account (managed by SLIIT) doesn't allow creating service principals, which blocks automated GitHub Actions deployment.

## The Solution

Use a **hybrid approach**:

- ✅ GitHub Actions: Builds, tests, and publishes Docker images
- ✅ Local Azure CLI: Deploy containers to Azure (one-time setup)

This meets your assignment requirements for:

- ✅ Microservices architecture
- ✅ CI/CD pipeline (build & publish)
- ✅ Dockerization
- ✅ Cloud deployment

---

## Step 1: Let GitHub Actions Build & Publish (Already Works!)

Your workflow already:

1. Builds all 5 microservices ✅
2. Runs security scans ✅
3. Builds Docker images ✅
4. Pushes to GitHub Container Registry ✅

The only step that fails is Azure deployment (needs credentials).

---

## Step 2: Deploy to Azure Manually (One-Time Setup)

Since you're logged into Azure CLI, you can deploy directly:

### 2.1 Set Environment Variables

```powershell
$RG = "ctse-microservices-rg"
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$REGISTRY = "ghcr.io"
$REPO = "y4s1-projects/ctse-assignment-microservices-cloud-deployments"
$TAG = "main"  # Or specific tag from GitHub
```

### 2.2 Create Resource Group

```powershell
az group create --name $RG --location $LOCATION
```

### 2.3 Create Container Apps Environment

```powershell
az containerapp env create `
  --name $ENV_NAME `
  --resource-group $RG `
  --location $LOCATION
```

### 2.4 Deploy API Gateway (Public)

```powershell
az containerapp create `
  --name api-gateway `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/${REPO}/api-gateway:$TAG" `
  --target-port 8080 `
  --ingress external `
  --min-replicas 1 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi
```

### 2.5 Deploy Auth Service (Internal)

```powershell
az containerapp create `
  --name auth-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/${REPO}/auth-service:$TAG" `
  --target-port 8081 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi
```

### 2.6 Deploy Catalog Service (Internal)

```powershell
az containerapp create `
  --name catalog-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/${REPO}/catalog-service:$TAG" `
  --target-port 8082 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi
```

### 2.7 Deploy Order Service (Internal)

```powershell
az containerapp create `
  --name order-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/${REPO}/order-service:$TAG" `
  --target-port 8083 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi
```

### 2.8 Deploy Payment Service (Internal)

```powershell
az containerapp create `
  --name payment-service `
  --resource-group $RG `
  --environment $ENV_NAME `
  --image "$REGISTRY/${REPO}/payment-service:$TAG" `
  --target-port 8084 `
  --ingress internal `
  --min-replicas 0 `
  --max-replicas 2 `
  --cpu 0.5 `
  --memory 1Gi
```

---

## Step 3: Get Your Public URL

```powershell
az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv
```

This gives you something like: `api-gateway.xxx.eastus.azurecontainerapps.io`

---

## Step 4: Test Your Deployment

```powershell
# Get the URL
$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv

# Test health endpoint
curl https://$API_URL/actuator/health

# Open Swagger UI in browser
Start-Process "https://$API_URL/swagger-ui.html"
```

---

## Step 5: Check Container Status in Azure Portal

1. Go to **portal.azure.com**
2. Search for **"Container Apps"**
3. You should now see all 5 services!
4. Click on each to see:
   - Running status
   - Logs
   - Metrics
   - Application URL

---

## Updating Services After Code Changes

When you push to GitHub main branch:

1. GitHub Actions builds new Docker images ✅
2. Images pushed to GitHub Container Registry ✅
3. To deploy updates to Azure:

```powershell
# Update API Gateway
az containerapp update `
  --name api-gateway `
  --resource-group ctse-microservices-rg `
  --image "ghcr.io/y4s1-projects/ctse-assignment-microservices-cloud-deployments/api-gateway:main"

# Repeat for other services...
```

---

## For Your Assignment Report

**Explain the limitation:**

> "The university Azure for Students account restricts Azure AD permissions, preventing automated service principal creation required for GitHub Actions Azure deployment. To work around this limitation while still demonstrating CI/CD and cloud deployment capabilities, a hybrid approach was implemented:
>
> 1. CI/CD Pipeline handles build, test, containerization, and registry publishing
> 2. Azure CLI deploys containers to Azure Container Apps environment
>
> This demonstrates understanding of:
>
> - Microservices architecture and communication
> - Docker containerization
> - CI/CD pipeline design and implementation
> - Azure Container Apps deployment
> - Authentication and authorization challenges in cloud environments"

**This is actually a GOOD thing** - shows you understand:

- Real-world authentication constraints
- Multiple deployment approaches
- Problem-solving when faced with limitations

---

## Alternative: Contact SLIIT IT Department

Some universities provide service principals for student projects. You could email:

- SLIIT IT Department
- Your course instructor

And ask: _"Can I get an Azure service principal for my cloud deployment assignment?"_

They might have a process for this!

---

## Quick Deployment Script

Save this as `deploy-to-azure.ps1`:

```powershell
# Configuration
$RG = "ctse-microservices-rg"
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$REGISTRY = "ghcr.io"
$REPO = "y4s1-projects/ctse-assignment-microservices-cloud-deployments"
$TAG = "main"

Write-Host "🚀 Deploying CTSE Microservices to Azure..." -ForegroundColor Cyan

# Create Resource Group
Write-Host "`n📦 Creating resource group..." -ForegroundColor Yellow
az group create --name $RG --location $LOCATION | Out-Null

# Create Container Apps Environment
Write-Host "🌍 Creating Container Apps environment..." -ForegroundColor Yellow
az containerapp env create `
  --name $ENV_NAME `
  --resource-group $RG `
  --location $LOCATION | Out-Null

# Deploy Services
$services = @(
    @{name="api-gateway"; port=8080; ingress="external"; min=1},
    @{name="auth-service"; port=8081; ingress="internal"; min=0},
    @{name="catalog-service"; port=8082; ingress="internal"; min=0},
    @{name="order-service"; port=8083; ingress="internal"; min=0},
    @{name="payment-service"; port=8084; ingress="internal"; min=0}
)

foreach ($svc in $services) {
    Write-Host "🐳 Deploying $($svc.name)..." -ForegroundColor Yellow

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
      --memory 1Gi | Out-Null

    Write-Host "  ✅ $($svc.name) deployed" -ForegroundColor Green
}

# Get API Gateway URL
Write-Host "`n🌐 Getting API Gateway URL..." -ForegroundColor Cyan
$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv

Write-Host "`n✅ Deployment Complete!" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "🚀 API Gateway URL: https://$API_URL" -ForegroundColor Cyan
Write-Host "📊 Health Check: https://$API_URL/actuator/health" -ForegroundColor Cyan
Write-Host "📚 Swagger UI: https://$API_URL/swagger-ui.html" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green

Write-Host "`n📱 Check Azure Portal:" -ForegroundColor Yellow
Write-Host "   https://portal.azure.com -> Container Apps" -ForegroundColor Gray

Write-Host "`n💡 To update services after code changes, run:" -ForegroundColor Yellow
Write-Host '   .\deploy-to-azure.ps1' -ForegroundColor Gray
```

Then just run:

```powershell
.\deploy-to-azure.ps1
```

---

## Summary

You now have:

1. ✅ **GitHub Actions** building and publishing containers
2. ✅ **Manual deployment** to Azure Container Apps
3. ✅ **Public URL** for testing
4. ✅ **Full microservices** running in the cloud

This satisfies your assignment requirements and demonstrates real-world problem-solving! 🎓
