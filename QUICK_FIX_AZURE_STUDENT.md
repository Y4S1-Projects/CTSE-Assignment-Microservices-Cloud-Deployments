# Quick Fix: Deploy to Azure (Azure for Students Account)

## The Problem

Your Docker images are private in GitHub Container Registry, and Azure can't pull them without authentication.

## The Solution (2 Minutes)

### Step 1: Make Your GitHub Container Packages Public

1. Go to: **https://github.com/orgs/Y4S1-Projects/packages**

2. You should see 5 packages (if they exist):
   - `api-gateway`
   - `auth-service`
   - `catalog-service`
   - `order-service`
   - `payment-service`

3. For EACH package:
   - Click the package name
   - Click **"Package settings"** (on the right)
   - Scroll down to **"Danger Zone"**
   - Find **"Change package visibility"**
   - Click **"Change visibility"**
   - Select **"Public"**
   - Type the package name to confirm
   - Click **"I understand, change package visibility"**

Repeat for all 5 packages.

### Step 2: If Packages Don't Exist Yet

If you don't see any packages, it means GitHub Actions hasn't pushed them yet.

**Run this to trigger a build:**

```powershell
git commit --allow-empty -m "Trigger GitHub Actions build"
git push origin main
```

Then:

1. Go to: **https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments/actions**
2. Wait for the workflow to complete (5-10 minutes)
3. Then follow Step 1 above to make packages public

### Step 3: Deploy to Azure

Once packages are public, run:

```powershell
# Configuration
$RG = "ctse-microservices-rg"
$ENV_NAME = "ctse-env"
$REGISTRY = "ghcr.io"
$REPO = "y4s1-projects/ctse-assignment-microservices-cloud-deployments"
$TAG = "main"

# Deploy API Gateway (Public)
Write-Host "Deploying API Gateway..." -ForegroundColor Yellow
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
  --memory 1Gi

# Deploy Auth Service (Internal)
Write-Host "Deploying Auth Service..." -ForegroundColor Yellow
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
  --memory 1Gi

# Deploy Catalog Service (Internal)
Write-Host "Deploying Catalog Service..." -ForegroundColor Yellow
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
  --memory 1Gi

# Deploy Order Service (Internal)
Write-Host "Deploying Order Service..." -ForegroundColor Yellow
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
  --memory 1Gi

# Deploy Payment Service (Internal)
Write-Host "Deploying Payment Service..." -ForegroundColor Yellow
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
  --memory 1Gi

# Get API Gateway URL
Write-Host "`n[SUCCESS] Getting API Gateway URL..." -ForegroundColor Green
$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RG `
  --query "properties.configuration.ingress.fqdn" `
  --output tsv

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "API Gateway URL: https://$API_URL" -ForegroundColor Cyan
Write-Host "Health Check: https://$API_URL/actuator/health" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Green
```

Or save this entire script as `deploy-now.ps1` and run:

```powershell
.\deploy-now.ps1
```

---

## Summary of What We Did

1. ✅ Created resource group in Southeast Asia (allowed region for your university account)
2. ✅ Registered required Azure providers (Microsoft.App, Microsoft.OperationalInsights)
3. ✅ Created Container Apps environment
4. ⏳ Need to make GitHub packages public (you need to do this)
5. ⏳ Then deploy services

## Why This Happened

**Azure for Students limitations:**

- 🚫 Can't create service principals (Azure AD restricted by university)
- 🚫 Can't access App Registrations (Azure AD restricted)
- 🚫 Some resource providers require manual registration
- 🚫 Some regions are restricted by university IT policies

**Solution:**

- Use **Southeast Asia** region instead of East US
- Make GitHub packages **public** (no authentication needed)
- Deploy with **local Azure CLI** (you're already logged in)
- Keep GitHub Actions for building (works great)

This approach still demonstrates:

- ✅ Microservices architecture
- ✅ Docker containerization
- ✅ CI/CD pipeline (build & test)
- ✅ Cloud deployment on Azure
- ✅ Problem-solving with real-world constraints

Perfect for your assignment! 🎓
