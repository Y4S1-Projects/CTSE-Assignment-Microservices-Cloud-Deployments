# ⚡ COPY & PASTE Azure Deployment Commands
# These are ready to use - just update the variables and run in PowerShell

## STEP 1: Set Variables
$RESOURCE_GROUP = "ctse-microservices-rg"
$REGISTRY_NAME = "ctsereg2026"  # ⚠️ MUST BE UNIQUE - change the number
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"
$JWT_SECRET = "your-secret-key-2026"

## STEP 2: Create Resource Group
az group create --name $RESOURCE_GROUP --location $LOCATION

## STEP 3: Create Container Registry
az acr create --name $REGISTRY_NAME --resource-group $RESOURCE_GROUP --sku Basic

## STEP 4: Build Images in Azure (5-10 min)
# This uses Azure's infrastructure - no local Docker needed
az acr build --registry $REGISTRY_NAME --image auth-service:latest ./auth-service
az acr build --registry $REGISTRY_NAME --image catalog-service:latest ./catalog-service
az acr build --registry $REGISTRY_NAME --image order-service:latest ./order-service
az acr build --registry $REGISTRY_NAME --image payment-service:latest ./payment-service
az acr build --registry $REGISTRY_NAME --image api-gateway:latest ./api-gateway

## STEP 5: Create Container Apps Environment
az containerapp env create --name $ENV_NAME --resource-group $RESOURCE_GROUP --location $LOCATION

## STEP 6: Deploy Auth Service
az containerapp create `
  --name auth-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/auth-service:latest" `
  --target-port 8081 `
  --cpu 0.5 --memory 1Gi

## STEP 7: Deploy Catalog Service
az containerapp create `
  --name catalog-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/catalog-service:latest" `
  --target-port 8082 `
  --cpu 0.5 --memory 1Gi

## STEP 8: Deploy Order Service
az containerapp create `
  --name order-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/order-service:latest" `
  --target-port 8083 `
  --cpu 0.5 --memory 1Gi

## STEP 9: Deploy Payment Service
az containerapp create `
  --name payment-service `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/payment-service:latest" `
  --target-port 8084 `
  --cpu 0.5 --memory 1Gi

## STEP 10: Deploy API Gateway (with external URL)
az containerapp create `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --environment $ENV_NAME `
  --image "$REGISTRY_NAME.azurecr.io/api-gateway:latest" `
  --target-port 8080 `
  --ingress external `
  --environment-variables `
    JWT_SECRET=$JWT_SECRET `
    AUTH_SERVICE_URL="http://auth-service" `
    CATALOG_SERVICE_URL="http://catalog-service" `
    ORDER_SERVICE_URL="http://order-service" `
    PAYMENT_SERVICE_URL="http://payment-service" `
  --cpu 0.5 --memory 1Gi

## STEP 11: Get the API Gateway URL (run this to get your endpoint)
az containerapp show `
  --name api-gateway `
  --resource-group $RESOURCE_GROUP `
  --query "properties.configuration.ingress.fqdn" -o tsv

## STEP 12: Test the API
# Replace {URL} with the output from Step 11
curl https://{URL}/actuator/health

## STEP 13: View Logs
az containerapp logs show --name api-gateway --resource-group $RESOURCE_GROUP --follow

## STEP 14: List all services
az containerapp list --resource-group $RESOURCE_GROUP --output table

## CLEANUP: Delete everything
az group delete --name $RESOURCE_GROUP --yes
