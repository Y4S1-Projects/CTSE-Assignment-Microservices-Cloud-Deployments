# ⚡ Express Azure Deployment Guide

## Quick Deploy (5 minutes)

If you're experiencing Docker build issues locally, Azure Container Registry can build images for you. Here's the fastest path:

### Option 1: Build in Azure (Recommended - Faster)

```powershell
# Variables
$RESOURCE_GROUP = "ctse-microservices-rg"
$REGISTRY_NAME = "ctseregistry2026"  # Change to unique name
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$JWT_SECRET = "your-secret-key-123"  # Change in production

# 1. Create Resource Group
az group create --name $RESOURCE_GROUP --location $LOCATION

# 2. Create Container Registry
az acr create `
  --resource-group $RESOURCE_GROUP `
  --name $REGISTRY_NAME `
  --sku Basic

# 3. Build images in Azure (faster, uses Azure's resources)
az acr build --registry $REGISTRY_NAME --image api-gateway:latest ./api-gateway
az acr build --registry $REGISTRY_NAME --image auth-service:latest ./auth-service
az acr build --registry $REGISTRY_NAME --image catalog-service:latest ./catalog-service
az acr build --registry $REGISTRY_NAME --image order-service:latest ./order-service
az acr build --registry $REGISTRY_NAME --image payment-service:latest ./payment-service

# 4. Create Container Apps Environment
az containerapp env create `
  --name $ENV_NAME `
  --resource-group $RESOURCE_GROUP `
  --location $LOCATION

# 5. Deploy Auth Service
az containerapp create `
  --name auth-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/auth-service:latest" `
  --target-port 8081 `
  --registry-server "$REGISTRY_NAME.azurecr.io" `
  --ingress internal `
  --cpu 0.5 --memory 1Gi

# 6. Deploy Catalog Service
az containerapp create `
  --name catalog-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/catalog-service:latest" `
  --target-port 8082 `
  --registry-server "$REGISTRY_NAME.azurecr.io" `
  --ingress internal `
  --cpu 0.5 --memory 1Gi

# 7. Deploy Order Service
az containerapp create `
  --name order-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/order-service:latest" `
  --target-port 8083 `
  --registry-server "$REGISTRY_NAME.azurecr.io" `
  --ingress internal `
  --cpu 0.5 --memory 1Gi

# 8. Deploy Payment Service
az containerapp create `
  --name payment-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/payment-service:latest" `
  --target-port 8084 `
  --registry-server "$REGISTRY_NAME.azurecr.io" `
  --ingress internal `
  --cpu 0.5 --memory 1Gi

# 9. Deploy API Gateway (with external ingress)
az containerapp create `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/api-gateway:latest" `
  --target-port 8080 `
  --registry-server "$REGISTRY_NAME.azurecr.io" `
  --ingress external `
  --environment-variables `
    JWT_SECRET=$JWT_SECRET `
    SERVICE_AUTH_URL="http://auth-service" `
    SERVICE_CATALOG_URL="http://catalog-service" `
    SERVICE_ORDER_URL="http://order-service" `
    SERVICE_PAYMENT_URL="http://payment-service" `
  --cpu 0.5 --memory 1Gi

# 10. Get API Gateway URL
az containerapp show `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --query "properties.configuration.ingress.fqdn" -o tsv
```

## Advantages of this approach:

✅ **No local Docker build required** - Azure builds images for you  
✅ **Faster** - Uses Azure's parallel build infrastructure  
✅ **Simpler** - Fewer local dependencies  
✅ **Managed** - All in Azure cloud

## If you prefer local build, fix the issue first:

```bash
# Test individual service builds
cd api-gateway && docker build . && cd ..
cd auth-service && docker build . && cd ..
cd catalog-service && docker build . && cd ..
cd order-service && docker build . && cd ..
cd payment-service && docker build . && cd ..
```

If any fail, it's likely a missing dependency or port issue. The services can then be manually fixed before deployment.

## Cost

- **Container Registry**: ~$5/month
- **Container Apps**: ~$3-10/month per service (varies by usage)
- **First 2 million ACR builds** are free per month

Total estimated cost: **$20-60/month** for this system.

## Next Steps

1. Copy the PowerShell commands above
2. Update the variables (unique registry name, JWT secret)
3. Run commands in PowerShell
4. Wait for Azure to build and deploy
5. Test at the returned API Gateway URL

---

**Note**: Azure Container Apps uses the Dockerfile in each service directory, same as Docker Compose, so no changes needed to your code.
