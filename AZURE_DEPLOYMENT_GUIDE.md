# Azure Deployment Guide - University Account Setup

## 🎓 University Email Limitation

**Issue**: University emails often have restricted permissions for Azure AD operations (creating service principals, app registrations).

**Solution**: Use GitHub Actions with manual Azure credential setup OR manual deployment scripts.

---

## ✅ Current Setup (GitHub Actions)

### How It Works

1. **Push to `main` branch** → Triggers GitHub Actions workflow
2. **Build & Test** → Maven builds all services
3. **Security Scan** → Trivy scans for vulnerabilities
4. **Build Docker Images** → Pushes to GitHub Container Registry (ghcr.io)
5. **Deploy to Azure** → Uses `AZURE_CREDENTIALS` secret (service principal)

### Required GitHub Secrets

Go to **GitHub Repository → Settings → Secrets and variables → Actions**

| Secret Name | Description | How to Get |
|-------------|-------------|------------|
| `AZURE_CREDENTIALS` | Service Principal JSON | See "Manual Setup" below |
| `JWT_SECRET` | JWT signing key | Any secure random string (min 32 chars) |
| `AZURE_REGISTRY_URL` | Container Registry URL | `ghcr.io` (already set) |

---

## 🔧 Manual Setup (One-Time)

### Option 1: Ask Azure Admin (Recommended)

If your university has an Azure administrator, ask them to:

1. Create a Service Principal with these permissions:
   - **Contributor** role on your subscription
   - **AcrPush** role on Container Registry (if using ACR)

2. Get the JSON output and add it as `AZURE_CREDENTIALS` GitHub secret:

```json
{
  "clientId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "clientSecret": "your-secret-here",
  "subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "tenantId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### Option 2: Use Personal Azure Account

1. **Login with your personal Microsoft account** (not university email):
   ```bash
   az login
   ```

2. **Create Service Principal**:
   ```bash
   az ad sp create-for-rbac \
     --name "github-actions-ctse" \
     --role contributor \
     --scopes /subscriptions/YOUR_SUBSCRIPTION_ID \
     --sdk-auth
   ```

3. **Copy the JSON output** → Add to GitHub Secrets as `AZURE_CREDENTIALS`

4. **Setup JWT Secret**:
   ```bash
   # Generate random string
   openssl rand -base64 32
   ```
   Add output to GitHub Secrets as `JWT_SECRET`

---

## 🚀 Deployment Process

### Automatic (via GitHub Actions)

```bash
# Just push to main
git add .
git commit -m "Deploy updates"
git push origin main
```

**GitHub Actions will:**
- ✅ Build all services
- ✅ Run tests
- ✅ Scan for security issues
- ✅ Build Docker images
- ✅ Deploy to Azure Container Apps
- ✅ Run smoke tests

### Manual Deployment (Fallback)

If GitHub Actions fails due to permission issues:

```powershell
# Login to Azure (use personal account)
az login

# Run deployment script
./deploy-azure.ps1
```

**Or using manual commands**:

```powershell
# Set variables
$RG = "ctse-microservices-rg"
$LOCATION = "eastus"
$ENV_NAME = "ctse-env"

# Create resource group
az group create --name $RG --location $LOCATION

# Create container apps environment
az containerapp env create \
  --name $ENV_NAME \
  --resource-group $RG \
  --location $LOCATION

# Deploy services (example: auth-service)
az containerapp create \
  --name auth-service \
  --resource-group $RG \
  --environment $ENV_NAME \
  --image ghcr.io/y4s1-projects/ctse-assignment-microservices-cloud-deployments/auth-service:latest \
  --target-port 8081 \
  --ingress internal \
  --env-vars JWT_SECRET="your-jwt-secret-here" \
  --cpu 0.5 --memory 1.0Gi

# Deploy API Gateway with service URLs
az containerapp create \
  --name api-gateway \
  --resource-group $RG \
  --environment $ENV_NAME \
  --image ghcr.io/y4s1-projects/ctse-assignment-microservices-cloud-deployments/api-gateway:latest \
  --target-port 8080 \
  --ingress external \
  --env-vars \
    JWT_SECRET="your-jwt-secret-here" \
    AUTH_SERVICE_URL="http://auth-service" \
    CATALOG_SERVICE_URL="http://catalog-service" \
    ORDER_SERVICE_URL="http://order-service" \
    PAYMENT_SERVICE_URL="http://payment-service" \
  --cpu 0.5 --memory 1.0Gi \
  --min-replicas 2 --max-replicas 5

# Get API Gateway URL
az containerapp show \
  --name api-gateway \
  --resource-group $RG \
  --query "properties.configuration.ingress.fqdn" \
  --output tsv
```

---

## 🧪 Testing Deployment

### 1. Check Deployment Status

Go to **GitHub → Actions** tab to see:
- ✅ Build status
- ✅ Deployment logs
- ✅ API Gateway URL

### 2. Test Endpoints

```bash
# Get your Gateway URL from GitHub Actions or:
GATEWAY_URL=$(az containerapp show --name api-gateway --resource-group ctse-microservices-rg --query "properties.configuration.ingress.fqdn" -o tsv)

# Test health endpoint
curl https://$GATEWAY_URL/health

# Test auth service
curl https://$GATEWAY_URL/auth/health

# Test Swagger UI
# Open in browser: https://$GATEWAY_URL/swagger-ui.html

# Register a user
curl -X POST https://$GATEWAY_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User"
  }'
```

---

## 🔍 Troubleshooting

### GitHub Actions Fails with "Azure Login"

**Cause**: `AZURE_CREDENTIALS` secret is missing or invalid

**Fix**:
1. Check if secret exists: GitHub → Settings → Secrets → Actions
2. Verify JSON format (must be valid service principal JSON)
3. Use manual deployment script instead

### "Permission Denied" with University Email

**Cause**: University accounts have restricted Azure AD permissions

**Fix**:
1. Use personal Microsoft account for Azure login
2. OR ask university Azure admin for service principal
3. OR use manual deployment with personal account

### Services Not Communicating

**Cause**: Environment variables not set correctly

**Fix**:
1. Check GitHub Actions logs for deployment step
2. Verify environment variables in Azure Portal:
   - Go to Container App → Settings → Environment variables
3. Ensure service URLs use internal names (e.g., `http://auth-service`)

### CORS Errors in Production

**Cause**: Swagger trying to call services directly

**Fix**: Already fixed! Services use relative URLs, so all requests go through API Gateway

---

## 📊 Deployment Checklist

Before pushing to `main`:

- [ ] All services build successfully locally
- [ ] `AZURE_CREDENTIALS` secret is set in GitHub
- [ ] `JWT_SECRET` secret is set in GitHub (min 32 characters)
- [ ] Tested locally with `docker-compose up`
- [ ] API Gateway configuration has environment variable defaults
- [ ] All CORS configurations allow gateway origin

After deployment:

- [ ] Check GitHub Actions for green checkmarks
- [ ] Test `/health` endpoint
- [ ] Test `/auth/health` endpoint
- [ ] Open Swagger UI and test registration
- [ ] Verify all services are running in Azure Portal

---

## 🎯 Key Configuration for Azure

### API Gateway (`application.properties`)

```properties
# Uses environment variables with localhost defaults
spring.cloud.gateway.routes[0].uri=${AUTH_SERVICE_URL:http://localhost:8081}
spring.cloud.gateway.routes[1].uri=${CATALOG_SERVICE_URL:http://localhost:8082}
# ... etc
```

### Auth Service (`SecurityConfig.java`)

```java
// CORS allows all origins for development
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowCredentials(true);
```

### GitHub Actions (`deploy.yml`)

```yaml
# Automatically sets environment variables for Azure
--env-vars \
  JWT_SECRET=${{ secrets.JWT_SECRET }} \
  AUTH_SERVICE_URL=http://auth-service \
  CATALOG_SERVICE_URL=http://catalog-service \
  ORDER_SERVICE_URL=http://order-service \
  PAYMENT_SERVICE_URL=http://payment-service
```

---

## 📞 Support

If you encounter issues:

1. **Check GitHub Actions logs**: Click on failed workflow → Click on failed step
2. **Check Azure Portal logs**: Container App → Monitoring → Log stream
3. **Test locally first**: `mvn clean package && java -jar target/*.jar`
4. **Verify secrets**: GitHub → Settings → Secrets → Actions

---

## ✅ Success Indicators

Your deployment is successful when:

1. ✅ GitHub Actions workflow completes with all green checkmarks
2. ✅ Gateway URL is displayed in GitHub Actions summary
3. ✅ `curl https://<gateway-url>/health` returns `{"status":"UP"}`
4. ✅ Swagger UI loads at `https://<gateway-url>/swagger-ui.html`
5. ✅ Can register a user via `/auth/register`

---

## 🚀 Next Steps After Successful Deployment

1. **Save your Gateway URL** - You'll need it for testing
2. **Test all endpoints** - Use Swagger UI
3. **Monitor logs** - Check Azure Portal for any errors
4. **Setup custom domain** (optional) - In Azure Container Apps settings
5. **Configure production secrets** - Change JWT_SECRET to strong production value
