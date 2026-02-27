# ✅ Fixed: GitHub Actions Deployment Without Service Principal Permissions

## Summary of Changes

I've completely redesigned the deployment workflow to work **without requiring service principal creation permissions** - perfect for university Azure accounts!

---

## 🎯 What Changed

### 1. GitHub Actions Workflow Updated

**File:** [.github/workflows/deploy.yml](.github/workflows/deploy.yml)

- ✅ **Builds Docker images** automatically when you push to `main`
- ✅ **Pushes to GitHub Container Registry** (ghcr.io)
- ⏭️ **Skips Azure deployment** if `AZURE_CREDENTIALS` not configured
- ✅ **Shows helpful message** with manual deployment instructions

### 2. New Deployment Script Created

**File:** [deploy-from-github.ps1](deploy-from-github.ps1)

- ✅ Uses **YOUR Azure login** (no service principal needed)
- ✅ Deploys **pre-built images from GitHub**
- ✅ No Docker required on your machine
- ✅ Auto-generates JWT secret if not provided
- ✅ Single command deployment: `.\deploy-from-github.ps1`

### 3. Documentation Updated

**Files:**

- [GITHUB_ACTIONS_FIX.md](GITHUB_ACTIONS_FIX.md) - Complete guide for no-permission deployment
- [README.md](README.md) - Added quick deploy section at top of deployment guide

---

## 🚀 How to Deploy Now

### The New Workflow

```
Step 1: YOU ────────────────> GitHub
        git push origin main

Step 2: GITHUB ACTIONS ─────> Builds all Docker images
        ✅ api-gateway
        ✅ auth-service
        ✅ catalog-service
        ✅ order-service
        ✅ payment-service
        📦 Pushed to ghcr.io

Step 3: YOU ────────────────> Azure
        az login
        .\deploy-from-github.ps1

Result: ✅ Live on Azure!
        https://api-gateway.xxx.azurecontainerapps.io
```

---

## 📝 Step-by-Step Instructions

### First Time Deployment

#### 1. **Push to GitHub (Builds Images)**

```powershell
# Make sure all changes are committed
git add .
git commit -m "Deploy to Azure"
git push origin main
```

**Wait for GitHub Actions to complete** (~5-10 minutes)

- Go to: https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments/actions
- Watch the workflow run
- ✅ `build-docker` job must succeed
- ⏭️ `manual-deployment-notice` will show deployment instructions

#### 2. **Deploy to Azure (One Command)**

```powershell
# Login with your university Azure account
az login

# Deploy (uses images from GitHub)
.\deploy-from-github.ps1
```

**That's it!** The script will:

- ✅ Create resource group (if needed)
- ✅ Create Container Apps environment (if needed)
- ✅ Deploy all 5 services
- ✅ Configure environment variables
- ✅ Show you the API Gateway URL

#### 3. **Test Your Deployment**

```powershell
# The script will show you the URL, use it like this:
$gateway = "YOUR-URL-HERE.azurecontainerapps.io"

# Test health endpoint
curl "https://$gateway/health"

# Test auth service via gateway
curl "https://$gateway/auth/health"

# Register a test user
$body = @{
    username = "testuser"
    email = "test@example.com"
    password = "password123"
    fullName = "Test User"
} | ConvertTo-Json

curl -X POST "https://$gateway/auth/register" `
    -H "Content-Type: application/json" `
    -d $body
```

---

## 🔄 Updating Your Deployment

When you make code changes:

```powershell
# 1. Push changes (rebuilds images automatically)
git add .
git commit -m "Update feature X"
git push origin main

# 2. Wait for GitHub Actions to complete

# 3. Redeploy (uses new images)
az login  # If not already logged in
.\deploy-from-github.ps1
```

---

## 🎓 Why This Works for University Accounts

| Method               | Permissions Needed   | Works with University Account? |
| -------------------- | -------------------- | ------------------------------ |
| ❌ Service Principal | Create Azure AD apps | ❌ NO - Blocked by IT          |
| ✅ Your Azure Login  | Basic Azure access   | ✅ YES - Works!                |

**The new method uses YOUR credentials** when you run `az login`, so no special permissions are needed!

---

## 🛠️ Advanced Options

### Deploy Specific Commit

```powershell
# Deploy images from specific commit SHA
.\deploy-from-github.ps1 -ImageTag "abc1234"
```

### Custom Resource Group

```powershell
# Use different resource group name
.\deploy-from-github.ps1 -ResourceGroup "my-project-rg" -Location "westeurope"
```

### Set Custom JWT Secret

```powershell
# Provide your own JWT secret
.\deploy-from-github.ps1 -JwtSecret "my-super-secret-key-minimum-32-characters-long"
```

### View Deployed Apps

```powershell
# List all container apps
az containerapp list --resource-group ctse-microservices-rg --output table

# Get API Gateway URL
az containerapp show `
    --name api-gateway `
    --resource-group ctse-microservices-rg `
    --query properties.configuration.ingress.fqdn `
    -o tsv
```

### Delete Everything

```powershell
# Remove all Azure resources
az group delete --name ctse-microservices-rg --yes --no-wait
```

---

## 📋 Deployment Checklist

Before deploying:

- [ ] Code pushed to `main` branch
- [ ] GitHub Actions build succeeded
- [ ] Azure CLI installed (`az --version`)
- [ ] Logged in to Azure (`az login`)

After deploying:

- [ ] API Gateway URL retrieved
- [ ] Health endpoint returns 200 OK
- [ ] Auth service registers users successfully
- [ ] Swagger UI accessible

---

## ❓ Troubleshooting

### Issue: "az login" fails

**Solution:**

```powershell
# Install Azure CLI if missing
winget install Microsoft.AzureCLI

# Or download from:
# https://learn.microsoft.com/en-us/cli/azure/install-azure-cli-windows
```

### Issue: "Cannot pull image from ghcr.io"

**Solution:** Make GitHub packages public

1. Go to: https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments
2. Click **Packages** (right sidebar)
3. For each package (api-gateway, auth-service, etc.):
   - Click **Package settings**
   - Scroll to **Danger Zone**
   - Click **Change visibility** → **Public**

### Issue: GitHub Actions build fails

**Solution:** Check the Actions tab for errors

- Go to: https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments/actions
- Click the failed workflow
- Check the logs for errors
- Common issues:
  - Maven build errors: Fix Java code
  - Docker build errors: Fix Dockerfile
  - Registry push errors: Check repository permissions

### Issue: Container app won't start

**Solution:** Check logs

```powershell
# View container app logs
az containerapp logs show `
    --name auth-service `
    --resource-group ctse-microservices-rg `
    --follow
```

---

## 📚 Related Documentation

- **[GITHUB_ACTIONS_FIX.md](GITHUB_ACTIONS_FIX.md)** - Detailed explanation of the no-permission solution
- **[deploy-from-github.ps1](deploy-from-github.ps1)** - The deployment script (with comments)
- **[README.md](README.md)** - Main project documentation
- **[AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md)** - Comprehensive Azure deployment guide
- **[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)** - Pre/post deployment verification

---

## 🎉 Summary

**Old Way (Didn't Work):**

```
Create service principal (needs special permissions)
→ ❌ University accounts blocked
```

**New Way (Works!):**

```powershell
git push origin main          # GitHub builds images
az login                      # Your credentials
.\deploy-from-github.ps1     # Deploy!
```

**You're all set!** 🚀

No service principal needed. No special permissions required. Just push, login, and deploy.
