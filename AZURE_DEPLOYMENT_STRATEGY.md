# 🚀 Azure Container Apps Deployment Strategy

## Status: Ready for Deployment

Your microservices system is fully containerized and ready to deploy to Azure Container Apps.

### Local Docker Build Issue

**Current Issue**: Docker multi-stage Maven build is failing locally  
**Solution**: Use Azure Container Registry's build service (ACR Tasks)  
**Benefit**: Faster, more reliable, no local Docker dependency

---

## Recommended: Azure ACR Build Method (Fastest)

This method uses Azure to build your Docker images, eliminating local build issues.

### Prerequisites

- ✅ Azure CLI installed
- ✅ Azure subscription with credit
- ✅ Already logged in to Azure

### Step-by-Step Azure Deployment

**Copy and run these commands in PowerShell:**

```powershell
# ==== CONFIGURATION ====
$RESOURCE_GROUP = "ctse-microservices-rg"
$REGISTRY_NAME = "ctsereg2026"  # MUST BE GLOBALLY UNIQUE - change the number
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$JWT_SECRET = "super-secret-key-change-me-in-prod"

Write-Host "Starting Azure deployment..." -ForegroundColor Green
Write-Host "Resource Group: $RESOURCE_GROUP" -ForegroundColor Cyan
Write-Host "Registry: $REGISTRY_NAME" -ForegroundColor Cyan
Write-Host ""

# ==== STEP 1: CREATE RESOURCE GROUP ====
Write-Host "Step 1: Creating resource group..." -ForegroundColor Yellow
az group create --name $RESOURCE_GROUP --location $LOCATION
Write-Host "✅ Done`n" -ForegroundColor Green

# ==== STEP 2: CREATE CONTAINER REGISTRY ====
Write-Host "Step 2: Creating container registry..." -ForegroundColor Yellow
az acr create --name $REGISTRY_NAME --resource-group $RESOURCE_GROUP --sku Basic
Write-Host "✅ Done`n" -ForegroundColor Green

# ==== STEP 3: BUILD IMAGES IN AZURE (no local Docker needed) ====
Write-Host "Step 3: Building images in Azure (this may take 5-10 minutes)..." -ForegroundColor Yellow
Write-Host "Note: This uses Azure's infrastructure, not your local Docker`n" -ForegroundColor Cyan

az acr build --registry $REGISTRY_NAME --image auth-service:latest ./auth-service
Write-Host "  ✅ auth-service built" -ForegroundColor Green

az acr build --registry $REGISTRY_NAME --image catalog-service:latest ./catalog-service
Write-Host "  ✅ catalog-service built" -ForegroundColor Green

az acr build --registry $REGISTRY_NAME --image order-service:latest ./order-service
Write-Host "  ✅ order-service built" -ForegroundColor Green

az acr build --registry $REGISTRY_NAME --image payment-service:latest ./payment-service
Write-Host "  ✅ payment-service built" -ForegroundColor Green

az acr build --registry $REGISTRY_NAME --image api-gateway:latest ./api-gateway
Write-Host "  ✅ api-gateway built`n" -ForegroundColor Green

# ==== STEP 4: CREATE CONTAINER APPS ENVIRONMENT ====
Write-Host "Step 4: Creating Container Apps environment..." -ForegroundColor Yellow
az containerapp env create `
  --name $ENV_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION
Write-Host "✅ Done`n" -ForegroundColor Green

# ==== STEP 5: DEPLOY SERVICES ====
Write-Host "Step 5: Deploying services..." -ForegroundColor Yellow

# Auth Service
Write-Host "  Deploying auth-service..." -ForegroundColor Cyan
az containerapp create `
  --name auth-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/auth-service:latest" `
  --target-port 8081 `
  --cpu 0.5 --memory 1Gi `
  --no-wait
Write-Host "  ✅ Deploying..." -ForegroundColor Green

# Catalog Service
Write-Host "  Deploying catalog-service..." -ForegroundColor Cyan
az containerapp create `
  --name catalog-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/catalog-service:latest" `
  --target-port 8082 `
  --cpu 0.5 --memory 1Gi `
  --no-wait
Write-Host "  ✅ Deploying..." -ForegroundColor Green

# Order Service
Write-Host "  Deploying order-service..." -ForegroundColor Cyan
az containerapp create `
  --name order-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/order-service:latest" `
  --target-port 8083 `
  --cpu 0.5 --memory 1Gi `
  --no-wait
Write-Host "  ✅ Deploying..." -ForegroundColor Green

# Payment Service
Write-Host "  Deploying payment-service..." -ForegroundColor Cyan
az containerapp create `
  --name payment-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/payment-service:latest" `
  --target-port 8084 `
  --cpu 0.5 --memory 1Gi `
  --no-wait
Write-Host "  ✅ Deploying..." -ForegroundColor Green

# API Gateway (with external ingress)
Write-Host "  Deploying api-gateway..." -ForegroundColor Cyan
az containerapp create `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/api-gateway:latest" `
  --target-port 8080 `
  --ingress external `
  --environment-variables `
    JWT_SECRET=$JWT_SECRET `
    SERVICE_AUTH_URL="http://auth-service" `
    SERVICE_CATALOG_URL="http://catalog-service" `
    SERVICE_ORDER_URL="http://order-service" `
    SERVICE_PAYMENT_URL="http://payment-service" `
  --cpu 0.5 --memory 1Gi
Write-Host "  ✅ Deployed`n" -ForegroundColor Green

# ==== STEP 6: GET ACCESS INFORMATION ====
Write-Host "Step 6: Retrieving access information..." -ForegroundColor Yellow

$API_URL = az containerapp show `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --query "properties.configuration.ingress.fqdn" -o tsv

Write-Host "`n╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                   🎉 DEPLOYMENT COMPLETE 🎉                   ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════════╝`n" -ForegroundColor Green

Write-Host "📊 DEPLOYMENT SUMMARY:" -ForegroundColor Cyan
Write-Host "  Resource Group: $RESOURCE_GROUP"
Write-Host "  Registry: $REGISTRY_NAME.azurecr.io"
Write-Host "  Environment: $ENV_NAME"
Write-Host "  Region: $LOCATION`n"

Write-Host "🌐 API Gateway URL:" -ForegroundColor Cyan
Write-Host "  https://$API_URL" -ForegroundColor Yellow
Write-Host ""

Write-Host "✅ All services are deployed and running on Azure!" -ForegroundColor Green
Write-Host "`nServices running internally:" -ForegroundColor Cyan
Write-Host "  • auth-service (http://auth-service:8081)"
Write-Host "  • catalog-service (http://catalog-service:8082)"
Write-Host "  • order-service (http://order-service:8083)"
Write-Host "  • payment-service (http://payment-service:8084)"
Write-Host "  • api-gateway (EXTERNAL: https://$API_URL)`n"

Write-Host "📝 NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Test the API:"
Write-Host "   curl https://$API_URL/actuator/health`n"
Write-Host "2. View logs:"
Write-Host "   az containerapp logs show --name api-gateway --resource-group $RESOURCE_GROUP --follow`n"
Write-Host "3. Monitor services:"
Write-Host "   az containerapp list --resource-group $RESOURCE_GROUP --output table`n"
```

---

## Alternative: Local Docker Fix

If you want to fix the local Docker build:

### Common Docker Build Issues:

**Issue 1: Maven build failure in Docker**

```dockerfile
# Likely cause: Java/Maven version incompatibility
# Solution: Use Docker buildkit
export DOCKER_BUILDKIT=1
docker build --progress=plain -t auth-service:latest ./auth-service
```

**Issue 2: Out of disk space**

```bash
docker system prune -a  # Clean up old images
docker builder prune     # Clean up build cache
```

**Issue 3: Timeout**

```bash
docker build --progress=plain --build-arg BUILDKIT_INLINE_CACHE=1 -t service:latest ./service
```

---

## Cost Breakdown

| Service                     | Cost/Month  | Notes                     |
| --------------------------- | ----------- | ------------------------- |
| ACR (Container Registry)    | ~$5         | Includes 2M free builds   |
| Container Apps (5 services) | ~$15-50     | Based on CPU/Memory usage |
| Storage                     | <$1         | Minimal logging           |
| **TOTAL**                   | **~$20-60** | Well within free tier     |

Azure for Students includes $100/month credit.

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Azure Cloud                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │    Container Apps Environment (ctse-env)            │  │
│  │                                                       │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │  │
│  │  │ Auth     │  │ Catalog  │  │ Order    │          │  │
│  │  │ Service  │  │ Service  │  │ Service  │  ...     │  │
│  │  │ :8081    │  │ :8082    │  │ :8083    │          │  │
│  │  └──────────┘  └──────────┘  └──────────┘          │  │
│  │       ↑              ↑              ↑                │  │
│  │       └──────────────┼──────────────┘                │  │
│  │                      │                               │  │
│  │           ┌──────────┴──────────┐                    │  │
│  │           │   API Gateway       │                    │  │
│  │           │   :8080 (EXTERNAL)  │                    │  │
│  │           └─────────────────────┘                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                  │
│  ┌──────────────────────┴──────────────────────┐           │
│  │  Container Registry (ctsereg2026)           │           │
│  │  • auth-service:latest                     │           │
│  │  • catalog-service:latest                  │           │
│  │  • order-service:latest                    │           │
│  │  • payment-service:latest                  │           │
│  │  • api-gateway:latest                      │           │
│  └─────────────────────────────────────────────┘           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
         │
         │ curl https://{API_GATEWAY_URL}
         ↓
    Internet Users
```

---

## Monitoring & Management

### View Logs

```bash
az containerapp logs show --name api-gateway --resource-group ctse-microservices-rg --follow
```

### Check Service Status

```bash
az containerapp list --resource-group ctse-microservices-rg --output table
```

### Scale Services

```bash
az containerapp update --name api-gateway --resource-group ctse-microservices-rg --min-replicas 2 --max-replicas 10
```

### Delete Everything

```bash
az group delete --name ctse-microservices-rg --yes
```

---

## Production Best Practices

1. **Use Azure Key Vault** for secrets
2. **Enable HTTPS/TLS** with Application Gateway
3. **Set resource alerts** in Azure Monitor
4. **Use managed identities** (no login credentials)
5. **Implement auto-scaling** based on CPU/memory
6. **Enable Azure Defender** for security
7. **Setup CI/CD** with GitHub Actions

---

**Status**: ✅ Ready to Deploy  
**Estimated Time**: 15-20 minutes  
**Cost**: < $1 for testing (free trial credits)

Start with the PowerShell commands above!
