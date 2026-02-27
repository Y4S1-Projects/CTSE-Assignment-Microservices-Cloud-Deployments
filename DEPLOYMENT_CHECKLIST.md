# Production Deployment Checklist

## ✅ Pre-Deployment Checks

### 1. Local Testing Complete
- [ ] All services build: `mvn clean package`
- [ ] All services run locally
- [ ] API Gateway routes to all services correctly
- [ ] Swagger UI accessible at `http://localhost:8080/swagger-ui.html`
- [ ] Can register user via `/auth/register`
- [ ] Can login via `/auth/login`
- [ ] Auth service responds at `http://localhost:8080/auth/health`
- [ ] Gateway health endpoint works: `http://localhost:8080/health`

### 2. Code Quality
- [ ] No compilation errors
- [ ] No critical security warnings (Trivy scan)
- [ ] CORS configured to allow gateway origin
- [ ] Environment variables use fallback defaults

### 3. Configuration Review

#### API Gateway (`api-gateway/src/main/resources/application.properties`)
```properties
# ✅ Should use environment variables with localhost defaults
spring.cloud.gateway.routes[0].uri=${AUTH_SERVICE_URL:http://localhost:8081}
spring.cloud.gateway.routes[1].uri=${CATALOG_SERVICE_URL:http://localhost:8082}
spring.cloud.gateway.routes[2].uri=${ORDER_SERVICE_URL:http://localhost:8083}
spring.cloud.gateway.routes[3].uri=${PAYMENT_SERVICE_URL:http://localhost:8084}
```

#### Auth Service CORS (`auth-service/src/main/java/.../config/SecurityConfig.java`)
```java
// ✅ Should allow all origins for development
configuration.setAllowedOriginPatterns(List.of("*"));
configuration.setAllowCredentials(true);
```

#### Auth Service OpenAPI (`auth-service/src/main/java/.../config/OpenApiConfig.java`)
```java
// ✅ Should NOT have hardcoded server URL
return new OpenAPI()
    .info(info)
    .components(new Components().addSecuritySchemes("bearerAuth", securityScheme));
// ❌ Should NOT have: .servers(List.of(localServer))
```

---

## 🔐 GitHub Secrets Setup

Go to: **GitHub Repository → Settings → Secrets and variables → Actions → New repository secret**

### Required Secrets

| Secret Name | Value | Example |
|-------------|-------|---------|
| `AZURE_CREDENTIALS` | Service Principal JSON | See [AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md) |
| `JWT_SECRET` | Random 32+ char string | `openssl rand -base64 32` |

### Verify Secrets Exist
```bash
# In GitHub UI: Settings → Secrets → Actions
# You should see:
✅ AZURE_CREDENTIALS
✅ JWT_SECRET
```

---

## 🚀 Deployment Steps

### Step 1: Commit & Push
```bash
git add .
git commit -m "Deploy: Updated microservices configuration"
git push origin main
```

### Step 2: Monitor GitHub Actions
1. Go to **GitHub → Actions** tab
2. Click on the running workflow
3. Watch for:
   - ✅ Build & Test (all services)
   - ✅ Security Scan (Trivy)
   - ✅ Build Docker Images
   - ✅ Deploy to Azure
   - ✅ Smoke Tests

### Step 3: Get Deployment URL
After successful deployment, check the workflow summary for:
```
🌐 API Gateway URL
https://api-gateway--<random>.eastus.azurecontainerapps.io
```

---

## 🧪 Post-Deployment Testing

### 1. Health Checks
```bash
# Replace <gateway-url> with your actual URL
GATEWAY_URL="your-gateway-url.azurecontainerapps.io"

# Gateway health
curl https://$GATEWAY_URL/health

# Auth service health via gateway
curl https://$GATEWAY_URL/auth/health
```

Expected responses:
```json
{"status":"UP","service":"api-gateway"}
{"status":"UP","service":"auth-service"}
```

### 2. Test User Registration
```bash
curl -X POST https://$GATEWAY_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "produser",
    "email": "prod@example.com",
    "password": "SecurePass123!",
    "fullName": "Production User"
  }'
```

Expected response (200):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "produser",
  "userId": "generated-uuid"
}
```

### 3. Test Swagger UI
Open in browser:
```
https://<gateway-url>/swagger-ui.html
```

Should see:
- ✅ API Gateway docs
- ✅ Auth Service endpoints
- ✅ All other service endpoints
- ✅ Can execute requests without CORS errors

### 4. Test Login
```bash
curl -X POST https://$GATEWAY_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "prod@example.com",
    "password": "SecurePass123!"
  }'
```

---

## 🔍 Troubleshooting

### GitHub Actions Fails

#### "Azure Login Failed"
**Cause**: Using university email or missing `AZURE_CREDENTIALS` secret

**Fix**:
1. Check if secret exists in GitHub Settings
2. If missing, see [AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md)
3. Use manual deployment as fallback

#### "Image Pull Failed"
**Cause**: GitHub Container Registry authentication issue

**Fix**:
```bash
# Verify GITHUB_TOKEN has package write permissions
# In GitHub: Settings → Actions → General → Workflow permissions
# Select: "Read and write permissions"
```

#### "Resource Group Not Found"
**Cause**: First time deployment

**Fix**: The script creates it automatically, but you can pre-create:
```bash
az group create --name ctse-microservices-rg --location eastus
```

### Deployment Succeeds But Service Not Responding

#### Check Container Logs
```bash
# Gateway logs
az containerapp logs show \
  --name api-gateway \
  --resource-group ctse-microservices-rg \
  --follow

# Auth service logs
az containerapp logs show \
  --name auth-service \
  --resource-group ctse-microservices-rg \
  --follow
```

#### Check Environment Variables
```bash
# Verify gateway has service URLs
az containerapp show \
  --name api-gateway \
  --resource-group ctse-microservices-rg \
  --query "properties.template.containers[0].env"
```

Should show:
```json
[
  {"name": "JWT_SECRET", "value": "***"},
  {"name": "AUTH_SERVICE_URL", "value": "http://auth-service"},
  {"name": "CATALOG_SERVICE_URL", "value": "http://catalog-service"},
  ...
]
```

#### Check Service Status
```bash
# List all apps and their status
az containerapp list \
  --resource-group ctse-microservices-rg \
  --output table
```

### CORS Errors in Production

**Symptom**: Swagger UI shows CORS errors when testing

**Cause**: Auth service OpenAPI config has hardcoded server URL

**Fix**: Verify [auth-service OpenAPI config](auth-service/src/main/java/com/example/authservice/config/OpenApiConfig.java) does NOT have:
```java
// ❌ Remove this:
.servers(List.of(localServer))
```

**Correct version**:
```java
// ✅ Should be:
return new OpenAPI()
    .info(info)
    .components(...);  // No .servers() call
```

### Service Communication Fails

**Symptom**: Gateway returns 503 when calling auth service

**Possible Causes**:
1. Auth service not running
2. Wrong service URL in gateway config
3. Network isolation issue

**Debug**:
```bash
# Check if auth service is accessible from gateway
az containerapp exec \
  --name api-gateway \
  --resource-group ctse-microservices-rg \
  --command "curl http://auth-service/health"
```

---

## 📊 Success Metrics

Your deployment is successful when:

| Metric | Expected | Check Method |
|--------|----------|--------------|
| GitHub Actions | All ✅ | GitHub → Actions tab |
| Gateway Health | `{"status":"UP"}` | `curl https://<url>/health` |
| Auth Health | `{"status":"UP"}` | `curl https://<url>/auth/health` |
| Swagger UI | Loads without errors | Open in browser |
| User Registration | Returns token | Test with curl or Swagger |
| No CORS Errors | All requests work | Check browser console |

---

## 🛡️ Security Considerations

### For Production Use

1. **Change JWT Secret**
   ```bash
   # Generate strong secret
   openssl rand -base64 64
   
   # Update in GitHub Secrets
   ```

2. **Restrict CORS**
   Update `SecurityConfig.java`:
   ```java
   // ❌ Development (allow all)
   configuration.setAllowedOriginPatterns(List.of("*"));
   
   // ✅ Production (specific domain)
   configuration.setAllowedOrigins(List.of(
       "https://yourfrontend.com",
       "https://api.yourcompany.com"
   ));
   ```

3. **Enable HTTPS Only**
   ```bash
   # Azure automatically provides HTTPS
   # Disable HTTP by setting ingress to HTTPS only
   ```

4. **Add Rate Limiting**
   Already configured in API Gateway (100 req/min per IP)

5. **Monitor Logs**
   ```bash
   # Set up log analytics workspace
   az monitor log-analytics workspace create \
     --resource-group ctse-microservices-rg \
     --workspace-name ctse-logs
   ```

---

## 🔄 Rollback Procedure

If deployment fails:

```bash
# Rollback to previous version
az containerapp update \
  --name api-gateway \
  --resource-group ctse-microservices-rg \
  --image ghcr.io/y4s1-projects/ctse-assignment-microservices-cloud-deployments/api-gateway:previous-sha

# Or redeploy from previous commit
git checkout <previous-commit>
git push origin main --force
```

---

## 📝 Manual Deployment Fallback

If GitHub Actions fails completely:

```powershell
# See AZURE_DEPLOYMENT_GUIDE.md for complete manual steps
# Or run:
./deploy-azure.ps1
```

---

## ✅ Final Checklist

Before submitting:

- [ ] All GitHub Actions workflows pass
- [ ] Gateway URL is accessible
- [ ] Swagger UI loads without errors
- [ ] Can register and login users
- [ ] All health endpoints respond correctly
- [ ] No CORS errors in browser console
- [ ] Service-to-service communication works
- [ ] Checked Azure Portal for all running services
- [ ] Tested at least 3 different API endpoints
- [ ] Documentation updated with gateway URL

---

## 📞 Support Resources

- **GitHub Actions Logs**: Repository → Actions → Latest workflow
- **Azure Portal**: portal.azure.com → Container Apps
- **Deployment Guide**: [AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md)
- **Troubleshooting**: Check container logs in Azure Portal

---

**Last Updated**: February 2026  
**Status**: Production Ready ✅
