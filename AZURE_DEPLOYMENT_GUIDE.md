# Azure Container Apps Deployment Guide

Complete guide for deploying the Food Ordering Microservices to Azure Container Apps.

## 📝 Prerequisites

1. **Azure Account**: Active Azure subscription
2. **Azure CLI**: Version 2.45.0 or higher
3. **Docker**: Version 20.10 or higher (for local testing)
4. **GitHub Account**: For GitHub Container Registry (GHCR)
5. **Git**: For version control

## 🔑 Step 1: Set Up Azure Resources

### 1.1 Login to Azure

```bash
az login
```

### 1.2 Create Resource Group

```bash
# Define variables
RESOURCE_GROUP="ctse-rg"
LOCATION="southeastasia"
ENVIRONMENT_NAME="ctse-env"

# Create resource group
az group create \
  -n $RESOURCE_GROUP \
  -l $LOCATION
```

### 1.3 Create Container Apps Environment

```bash
az containerapp env create \
  -n $ENVIRONMENT_NAME \
  -g $RESOURCE_GROUP \
  -l $LOCATION
```

This will create:

- Log Analytics workspace for monitoring
- Azure Container Apps environment
- Virtual network for internal communication

## 📦 Step 2: Push Images to GitHub Container Registry (GHCR)

### 2.1 Create GitHub Personal Access Token (PAT)

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select scopes: `write:packages`, `read:packages`, `delete:packages`
4. Copy the token (you won't see it again)

### 2.2 Login to GHCR Locally

```bash
# Store your GitHub username and PAT
GITHUB_USERNAME="your-github-username"
GITHUB_TOKEN="your-pat-token"

# Login to GHCR
echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin
```

### 2.3 Build and Push Each Service

```bash
# Variables
REPO_NAME="food-ordering-system"  # or your repo name
GITHUB_USERNAME="your-github-username"
TAG="latest"

# Build and push API Gateway
docker build -t ghcr.io/$GITHUB_USERNAME/$REPO_NAME:api-gateway-$TAG ./api-gateway
docker push ghcr.io/$GITHUB_USERNAME/$REPO_NAME:api-gateway-$TAG

# Build and push Auth Service
docker build -t ghcr.io/$GITHUB_USERNAME/$REPO_NAME:auth-service-$TAG ./auth-service
docker push ghcr.io/$GITHUB_USERNAME/$REPO_NAME:auth-service-$TAG

# Build and push Catalog Service
docker build -t ghcr.io/$GITHUB_USERNAME/$REPO_NAME:catalog-service-$TAG ./catalog-service
docker push ghcr.io/$GITHUB_USERNAME/$REPO_NAME:catalog-service-$TAG

# Build and push Order Service
docker build -t ghcr.io/$GITHUB_USERNAME/$REPO_NAME:order-service-$TAG ./order-service
docker push ghcr.io/$GITHUB_USERNAME/$REPO_NAME:order-service-$TAG

# Build and push Payment Service
docker build -t ghcr.io/$GITHUB_USERNAME/$REPO_NAME:payment-service-$TAG ./payment-service
docker push ghcr.io/$GITHUB_USERNAME/$REPO_NAME:payment-service-$TAG
```

## 🚀 Step 3: Deploy Services to Container Apps

### 3.1 Configure Registry Access (Required for GHCR)

```bash
# Register the GitHub Container Registry with Container Apps identity
# This allows Container Apps to pull images from GHCR

REGISTRY_SERVER="ghcr.io"

az containerapp env registry set \
  --name $ENVIRONMENT_NAME \
  -g $RESOURCE_GROUP \
  --server $REGISTRY_SERVER \
  --username $GITHUB_USERNAME \
  --password $GITHUB_TOKEN
```

### 3.2 Create Container Apps

#### Auth Service

```bash
JWT_SECRET="your-super-secret-key-change-in-production-env"

az containerapp create \
  --name auth-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image ghcr.io/$GITHUB_USERNAME/$REPO_NAME:auth-service-$TAG \
  --target-port 8081 \
  --ingress external \
  --registry-username $GITHUB_USERNAME \
  --registry-password $GITHUB_TOKEN \
  --env-vars \
    JWT_SECRET=$JWT_SECRET \
  --cpu 0.25 \
  --memory 0.5Gi \
  --min-replicas 1 \
  --max-replicas 3
```

#### Catalog Service

```bash
AUTH_SERVICE_URL="https://auth-service.CONTAINER_APPS_DOMAIN.azurecontainerapps.io"

az containerapp create \
  --name catalog-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image ghcr.io/$GITHUB_USERNAME/$REPO_NAME:catalog-service-$TAG \
  --target-port 8082 \
  --ingress external \
  --registry-username $GITHUB_USERNAME \
  --registry-password $GITHUB_TOKEN \
  --env-vars \
    JWT_SECRET=$JWT_SECRET \
    SERVICE_AUTH_URL=$AUTH_SERVICE_URL \
  --cpu 0.25 \
  --memory 0.5Gi \
  --min-replicas 1 \
  --max-replicas 3
```

#### Order Service

```bash
CATALOG_SERVICE_URL="https://catalog-service.CONTAINER_APPS_DOMAIN.azurecontainerapps.io"
PAYMENT_SERVICE_URL="https://payment-service.CONTAINER_APPS_DOMAIN.azurecontainerapps.io"

az containerapp create \
  --name order-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image ghcr.io/$GITHUB_USERNAME/$REPO_NAME:order-service-$TAG \
  --target-port 8083 \
  --ingress external \
  --registry-username $GITHUB_USERNAME \
  --registry-password $GITHUB_TOKEN \
  --env-vars \
    JWT_SECRET=$JWT_SECRET \
    SERVICE_AUTH_URL=$AUTH_SERVICE_URL \
    SERVICE_CATALOG_URL=$CATALOG_SERVICE_URL \
    SERVICE_PAYMENT_URL=$PAYMENT_SERVICE_URL \
  --cpu 0.25 \
  --memory 0.5Gi \
  --min-replicas 1 \
  --max-replicas 3
```

#### Payment Service

```bash
ORDER_SERVICE_URL="https://order-service.CONTAINER_APPS_DOMAIN.azurecontainerapps.io"

az containerapp create \
  --name payment-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image ghcr.io/$GITHUB_USERNAME/$REPO_NAME:payment-service-$TAG \
  --target-port 8084 \
  --ingress external \
  --registry-username $GITHUB_USERNAME \
  --registry-password $GITHUB_TOKEN \
  --env-vars \
    JWT_SECRET=$JWT_SECRET \
    SERVICE_AUTH_URL=$AUTH_SERVICE_URL \
    SERVICE_ORDER_URL=$ORDER_SERVICE_URL \
  --cpu 0.25 \
  --memory 0.5Gi \
  --min-replicas 1 \
  --max-replicas 3
```

#### API Gateway

```bash
GATEWAY_URL="https://api-gateway.CONTAINER_APPS_DOMAIN.azurecontainerapps.io"

az containerapp create \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image ghcr.io/$GITHUB_USERNAME/$REPO_NAME:api-gateway-$TAG \
  --target-port 8080 \
  --ingress external \
  --registry-username $GITHUB_USERNAME \
  --registry-password $GITHUB_TOKEN \
  --env-vars \
    JWT_SECRET=$JWT_SECRET \
    SERVICE_AUTH_URL=$AUTH_SERVICE_URL \
    SERVICE_CATALOG_URL=$CATALOG_SERVICE_URL \
    SERVICE_ORDER_URL=$ORDER_SERVICE_URL \
    SERVICE_PAYMENT_URL=$PAYMENT_SERVICE_URL \
  --cpu 0.25 \
  --memory 0.5Gi \
  --min-replicas 1 \
  --max-replicas 3
```

### 3.3 Get Service URLs

```bash
# Get API Gateway URL (public entry point)
GATEWAY_URL=$(az containerapp show \
  -n api-gateway \
  -g $RESOURCE_GROUP \
  --query 'properties.configuration.ingress.fqdn' -o tsv)

echo "API Gateway: https://$GATEWAY_URL"

# Get individual service URLs (internal communication)
AUTH_SERVICE_URL=$(az containerapp show \
  -n auth-service \
  -g $RESOURCE_GROUP \
  --query 'properties.configuration.ingress.fqdn' -o tsv)

echo "Auth Service: https://$AUTH_SERVICE_URL"
```

## 🔄 Step 4: Update Service-to-Service URLs

Update environment variables with actual FQDN URLs:

```bash
# Update Order Service with internal URLs
az containerapp update \
  --name order-service \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    SERVICE_CATALOG_URL="https://catalog-service.$CONTAINER_APPS_DOMAIN" \
    SERVICE_PAYMENT_URL="https://payment-service.$CONTAINER_APPS_DOMAIN"

# Update Payment Service with internal URLs
az containerapp update \
  --name payment-service \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars \
    SERVICE_ORDER_URL="https://order-service.$CONTAINER_APPS_DOMAIN"
```

## 🧪 Step 5: Test Deployments

### Health Checks

```bash
# Test API Gateway health
curl https://$GATEWAY_URL/actuator/health

# Test Auth Service directly
curl https://$AUTH_SERVICE_URL/actuator/health

# Test Catalog Service directly
curl https://$CATALOG_SERVICE_URL/actuator/health

# Test Order Service directly
curl https://$ORDER_SERVICE_URL/actuator/health

# Test Payment Service directly
curl https://$PAYMENT_SERVICE_URL/actuator/health
```

### Test API Flow

```bash
# 1. Register user
curl -X POST https://$GATEWAY_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "email":"test@example.com",
    "password":"password123",
    "fullName":"Test User"
  }'

# 2. Login
RESPONSE=$(curl -X POST https://$GATEWAY_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"test@example.com",
    "password":"password123"
  }')

TOKEN=$(echo $RESPONSE | jq -r '.token')

# 3. Get menu items
curl -H "Authorization: Bearer $TOKEN" \
  https://$GATEWAY_URL/catalog/items

# 4. Create order
curl -X POST https://$GATEWAY_URL/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items":[{"itemId":"item-uuid","quantity":2}]
  }'
```

## 📊 Step 6: Set Up Monitoring

### View Logs

```bash
# View Auth Service logs
az containerapp logs show \
  -n auth-service \
  -g $RESOURCE_GROUP \
  --tail 50

# View all container app logs
az monitor log-analytics query \
  -w /subscriptions/SUBSCRIPTION_ID/resourcegroups/$RESOURCE_GROUP/providers/microsoft.operationalinsights/workspaces/ctse-workspace \
  -q "ContainerAppConsoleLogs_CL | order by TimeGenerated desc | limit 100"
```

### View Metrics

```bash
# Get CPU and memory usage
az monitor metrics list \
  -g $RESOURCE_GROUP \
  --offset 1h \
  --interval PT5M \
  --aggregation Average \
  --metrics CpuUsagePercentage MemoryUsagePercentage
```

## 🔐 Step 7: Security Configuration

### Set Up Secrets

```bash
# Store sensitive data as Container Apps secrets
az containerapp secret set \
  --name auth-service \
  --resource-group $RESOURCE_GROUP \
  --secrets jwt-secret=$JWT_SECRET

# Reference in environment variables
az containerapp update \
  --name auth-service \
  --resource-group $RESOURCE_GROUP \
  --set-env-vars JWT_SECRET=secretref:jwt-secret
```

### Configure Network Security

```bash
# Optional: Restrict ingress to Azure regions
az containerapp update \
  --name order-service \
  --resource-group $RESOURCE_GROUP \
  --ingress-allow-insecure false
```

## 🔄 Step 8: CI/CD Integration

### GitHub Actions with Azure OIDC

1. **Create Azure Service Principal**:

```bash
SUBSCRIPTION_ID=$(az account show --query id -o tsv)
APP_ID=$(az ad app create --display-name "ctse-deployment" --query appId -o tsv)

az role assignment create \
  --assignee $APP_ID \
  --role "Contributor" \
  --scope "/subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP"
```

2. **Add GitHub Secrets**:

```
AZURE_CLIENT_ID: <app-id>
AZURE_TENANT_ID: <tenant-id>
AZURE_SUBSCRIPTION_ID: <subscription-id>
AZURE_RESOURCE_GROUP: ctse-rg
```

3. **Update `.github/workflows/deploy.yml`** with resource group and service names

## 📈 Step 9: Scaling and Auto-scaling

### Configure Auto-scaling

```bash
# Auth Service - scale based on CPU
az containerapp update \
  --name auth-service \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 1 \
  --max-replicas 3 \
  --scale-rule-name cpu-rule \
  --scale-rule-type cpu \
  --scale-rule-metadata type=Utilization \
  --scale-rule-metadata value=70
```

### Manual Scaling

```bash
# Scale Order Service to 5 replicas
az containerapp update \
  --name order-service \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 5 \
  --max-replicas 10
```

## 🗑️ Cleanup

### Delete All Resources

```bash
# Delete resource group (deletes all resources)
az group delete -n $RESOURCE_GROUP --yes

# Or delete individual container apps
az containerapp delete -n auth-service -g $RESOURCE_GROUP
az containerapp delete -n catalog-service -g $RESOURCE_GROUP
az containerapp delete -n order-service -g $RESOURCE_GROUP
az containerapp delete -n payment-service -g $RESOURCE_GROUP
az containerapp delete -n api-gateway -g $RESOURCE_GROUP
```

## 📋 Deployment Checklist

- [ ] Azure CLI installed and authenticated
- [ ] GitHub PAT created with package permissions
- [ ] Images built and pushed to GHCR
- [ ] Resource group created
- [ ] Container Apps environment created
- [ ] Registry credentials configured
- [ ] Auth Service deployed and tested
- [ ] Catalog Service deployed and tested
- [ ] Order Service deployed and tested
- [ ] Payment Service deployed and tested
- [ ] API Gateway deployed and tested
- [ ] End-to-end flow tested
- [ ] Monitoring configured
- [ ] Secrets management set up
- [ ] CI/CD pipeline configured
- [ ] Documentation updated with live URLs

## 🆘 Troubleshooting

### Image Pull Failed

```bash
# Check registry credentials
az containerapp registry list -g $RESOURCE_GROUP

# Update registry credentials
az containerapp registry update \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --server ghcr.io \
  --username $GITHUB_USERNAME \
  --password $GITHUB_TOKEN
```

### Service Unavailable

```bash
# Check container app status
az containerapp show -n auth-service -g $RESOURCE_GROUP -o json | jq '.properties.runningStatus'

# Restart container app
az containerapp update -n auth-service -g $RESOURCE_GROUP --image <same-image>
```

### High Latency Between Services

1. Ensure services are in the same Container Apps Environment
2. Use internal service discovery (e.g., `http://auth-service:8081`)
3. Check CPU and memory allocation

---

**Last Updated**: February 2026
**Status**: Ready for Deployment
