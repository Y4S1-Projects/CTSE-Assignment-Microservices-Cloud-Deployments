# 🚀 Azure Container Apps Deployment Guide

This guide will walk you through deploying the Food Ordering System microservices to **Azure Container Apps**.

## Prerequisites

- ✅ Azure subscription ([create free account](https://azure.microsoft.com/en-us/free/))
- ✅ Azure CLI installed (`az --version`)
- ✅ Docker installed and running
- ✅ All services built locally

## Step 1: Login to Azure

```bash
az login
```

This will open a browser window for authentication. Choose your Azure subscription if prompted.

**Verify login:**

```bash
az account show
```

## Step 2: Set Configuration Variables

Replace these values with your own:

```powershell
# PowerShell
$RESOURCE_GROUP = "ctse-microservices-rg"
$REGISTRY_NAME = "ctseregistry"
$LOCATION = "eastus"
$ENVIRONMENT_NAME = "ctse-env"
$REGISTRY_DOMAIN = "$REGISTRY_NAME.azurecr.io"
```

Or for Bash:

```bash
RESOURCE_GROUP="ctse-microservices-rg"
REGISTRY_NAME="ctseregistry"
LOCATION="eastus"
ENVIRONMENT_NAME="ctse-env"
REGISTRY_DOMAIN="$REGISTRY_NAME.azurecr.io"
```

## Step 3: Create Azure Resources

### 3.1 Create Resource Group

```bash
az group create \
  --name $RESOURCE_GROUP \
  --location $LOCATION
```

### 3.2 Create Azure Container Registry

```bash
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name $REGISTRY_NAME \
  --sku Basic
```

**Enable admin access:**

```bash
az acr update \
  --name $REGISTRY_NAME \
  --admin-enabled true
```

**Get registry credentials:**

```bash
az acr credential show \
  --name $REGISTRY_NAME
```

Save the username and password - you'll need them later.

### 3.3 Login to Container Registry

```bash
az acr login --name $REGISTRY_NAME
```

Or use the credentials from above:

```bash
docker login $REGISTRY_DOMAIN \
  --username <username> \
  --password <password>
```

## Step 4: Build and Push Docker Images

### 4.1 Build All Services

```bash
docker-compose build
```

### 4.2 Tag Images for Registry

```powershell
# PowerShell
$REGISTRY_DOMAIN = "ctseregistry.azurecr.io"

docker tag api-gateway:latest "$REGISTRY_DOMAIN/api-gateway:latest"
docker tag auth-service:latest "$REGISTRY_DOMAIN/auth-service:latest"
docker tag catalog-service:latest "$REGISTRY_DOMAIN/catalog-service:latest"
docker tag order-service:latest "$REGISTRY_DOMAIN/order-service:latest"
docker tag payment-service:latest "$REGISTRY_DOMAIN/payment-service:latest"
```

### 4.3 Push Images to Registry

```bash
docker push $REGISTRY_DOMAIN/api-gateway:latest
docker push $REGISTRY_DOMAIN/auth-service:latest
docker push $REGISTRY_DOMAIN/catalog-service:latest
docker push $REGISTRY_DOMAIN/order-service:latest
docker push $REGISTRY_DOMAIN/payment-service:latest
```

**Verify images:**

```bash
az acr repository list --name $REGISTRY_NAME
```

## Step 5: Create Container Apps Environment

```bash
az containerapp env create \
  --name $ENVIRONMENT_NAME \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION
```

## Step 6: Deploy Microservices

### 6.1 Deploy Auth Service (No external ingress)

```bash
az containerapp create \
  --name auth-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $REGISTRY_DOMAIN/auth-service:latest \
  --target-port 8081 \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password> \
  --environment-variables JWT_SECRET="your-super-secret-key-change-in-production" \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 1 \
  --max-replicas 3
```

### 6.2 Deploy Catalog Service (No external ingress)

```bash
az containerapp create \
  --name catalog-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $REGISTRY_DOMAIN/catalog-service:latest \
  --target-port 8082 \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password> \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 1 \
  --max-replicas 3
```

### 6.3 Deploy Order Service (No external ingress)

```bash
az containerapp create \
  --name order-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $REGISTRY_DOMAIN/order-service:latest \
  --target-port 8083 \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password> \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 1 \
  --max-replicas 3
```

### 6.4 Deploy Payment Service (No external ingress)

```bash
az containerapp create \
  --name payment-service \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $REGISTRY_DOMAIN/payment-service:latest \
  --target-port 8084 \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password> \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 1 \
  --max-replicas 3
```

### 6.5 Deploy API Gateway (With external ingress)

```bash
az containerapp create \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --environment $ENVIRONMENT_NAME \
  --image $REGISTRY_DOMAIN/api-gateway:latest \
  --target-port 8080 \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password> \
  --ingress external \
  --environment-variables \
    JWT_SECRET="your-super-secret-key-change-in-production" \
    SERVICE_AUTH_URL="http://auth-service" \
    SERVICE_CATALOG_URL="http://catalog-service" \
    SERVICE_ORDER_URL="http://order-service" \
    SERVICE_PAYMENT_URL="http://payment-service" \
  --cpu 0.5 \
  --memory 1.0Gi \
  --min-replicas 2 \
  --max-replicas 5
```

## Step 7: Verify Deployment

### 7.1 List All Container Apps

```bash
az containerapp list \
  --resource-group $RESOURCE_GROUP \
  --output table
```

### 7.2 Check Service Status

```bash
# Check API Gateway
az containerapp show \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --query "properties.configuration.ingress.fqdn"

# Check service logs
az containerapp logs show \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --follow
```

### 7.3 Test API Endpoint

Get the API Gateway URL and test:

```bash
# Replace with actual URL
curl https://<api-gateway-url>.azurecontainers.io/actuator/health
```

## Step 8: Configure Service-to-Service Communication

Since services are in the same Container Apps environment, they can communicate using service names as hostnames:

- `http://auth-service` (port 8081)
- `http://catalog-service` (port 8082)
- `http://order-service` (port 8083)
- `http://payment-service` (port 8084)

These environment variables are already set in the API Gateway deployment.

## Step 9: Enable Auto-Scaling (Optional)

```bash
az containerapp update \
  --name api-gateway \
  --resource-group $RESOURCE_GROUP \
  --min-replicas 2 \
  --max-replicas 10
```

## Step 10: Setup Monitoring (Optional)

### Enable Container Apps Logging

```bash
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --workspace-name ctse-logs

# Link to Container Apps environment
WORKSPACE_ID=$(az monitor log-analytics workspace show \
  --resource-group $RESOURCE_GROUP \
  --workspace-name ctse-logs \
  --query id -o tsv)

az containerapp env update \
  --name $ENVIRONMENT_NAME \
  --resource-group $RESOURCE_GROUP \
  --logs-destination log-analytics \
  --logs-customer-id $WORKSPACE_ID
```

## Cleanup (if needed)

Delete all resources:

```bash
az group delete \
  --name $RESOURCE_GROUP \
  --yes
```

## Troubleshooting

### Container won't start

```bash
# Check logs
az containerapp logs show \
  --name <service-name> \
  --resource-group $RESOURCE_GROUP \
  --follow

# Check revision status
az containerapp revision list \
  --name <service-name> \
  --resource-group $RESOURCE_GROUP
```

### Services can't communicate

Ensure service names match exactly in environment variables. Use `http://<service-name>` without port in Azure Container Apps DNS.

### Registry authentication failed

```bash
# Re-login to registry
az acr login --name $REGISTRY_NAME

# Update container app credentials
az containerapp update \
  --name <service-name> \
  --resource-group $RESOURCE_GROUP \
  --registry-server $REGISTRY_DOMAIN \
  --registry-username <username> \
  --registry-password <password>
```

## Production Recommendations

1. **Never hardcode secrets** - Use Azure Key Vault
2. **Use managed identities** for registry access
3. **Enable HTTPS** with Azure Application Gateway or Front Door
4. **Set resource limits** to prevent runaway costs
5. **Monitor with Azure Monitor** and set up alerts
6. **Use separate environments** for dev, staging, production
7. **Implement CI/CD** with GitHub Actions or Azure Pipelines

## Cost Estimation

Approximate monthly costs (US East):

- Container Apps: ~$20-50/month (varies by usage)
- Container Registry: ~$5/month
- Log Analytics: ~$5-10/month

Free tier available with Azure free account (12 months).

---

For more information, see the main [README.md](README.md) or Azure Container Apps [documentation](https://learn.microsoft.com/en-us/azure/container-apps/).
