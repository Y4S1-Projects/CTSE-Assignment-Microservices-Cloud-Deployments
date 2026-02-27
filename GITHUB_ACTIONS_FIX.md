# GitHub Actions Deployment Fix - No Permissions Required

## Problem

GitHub Actions deployment fails with:

```
ERROR: Please run 'az login' to setup account.
Error: Process completed with exit code 1.
```

## Root Cause

University Azure accounts **don't have permissions** to create service principals (Azure AD restriction).

## ✅ SIMPLE SOLUTION (No Special Permissions Needed)

Since you can't create service principals, **GitHub Actions will build the Docker images only**. Then you deploy manually with your own Azure login:

---

## How It Works Now

### 1. GitHub Actions (Automatic)

When you push to `main`:

- ✅ Builds all 5 Docker images
- ✅ Pushes to GitHub Container Registry
- ⏭️ Skips Azure deployment (no AZURE_CREDENTIALS)
- ✅ Shows message: _"Manual deployment required"_

### 2. You Deploy Manually (One Command)

After GitHub Actions completes:

```powershell
# Just login and deploy
az login
.\deploy-from-github.ps1
```

**That's it!** No service principal, no special permissions needed.

---

## Step-by-Step Guide

### First Time Setup

#### 1. Verify GitHub Actions Works

```powershell
# Make any small change and push
git add .
git commit -m "Trigger build"
git push origin main
```

Go to: https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments/actions

You should see:

- ✅ `build-docker` job succeeds (builds images)
- ⏭️ `manual-deployment-notice` job shows deployment instructions
- ⏭️ `deploy-to-azure` job is skipped

#### 2. Deploy to Azure

```powershell
# Login with your university Azure account
az login

# Deploy using pre-built images from GitHub
.\deploy-from-github.ps1
```

The script will:

- ✅ Use images already built by GitHub Actions
- ✅ Create Azure resources using YOUR login (no service principal)
- ✅ Deploy all 5 microservices
- ✅ Give you the API Gateway URL

---

## Full Workflow

```
┌─────────────────────────────────────────────────────────┐
│ 1. YOU: Push code to GitHub                            │
│    git push origin main                                 │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│ 2. GITHUB ACTIONS: Builds Docker images automatically  │
│    ✅ api-gateway:sha123                                │
│    ✅ auth-service:sha123                               │
│    ✅ catalog-service:sha123                            │
│    ✅ order-service:sha123                              │
│    ✅ payment-service:sha123                            │
│    📦 Pushed to ghcr.io                                 │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│ 3. YOU: Deploy to Azure (from your computer)           │
│    az login                                             │
│    .\deploy-from-github.ps1                             │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│ 4. RESULT: Live on Azure!                              │
│    https://api-gateway.xxx.azurecontainerapps.io        │
└─────────────────────────────────────────────────────────┘
```

---

## Why This Works for University Accounts

| Method                        | Needs Service Principal? | University Account? |
| ----------------------------- | ------------------------ | ------------------- |
| ❌ GitHub Actions Auto Deploy | ✅ YES                   | ❌ Won't work       |
| ✅ Deploy from GitHub Script  | ❌ NO                    | ✅ Works!           |

**The new script (`deploy-from-github.ps1`) uses YOUR Azure login** - no service principal required!

---

## Script Options

### Basic Deployment (Uses latest images)

```powershell
.\deploy-from-github.ps1
```

### Deploy Specific Commit

```powershell
# Use images from a specific commit SHA
.\deploy-from-github.ps1 -ImageTag "abc1234"
```

### Custom Resource Group

```powershell
.\deploy-from-github.ps1 -ResourceGroup "my-custom-rg" -Location "westus"
```

### Set Custom JWT Secret

```powershell
.\deploy-from-github.ps1 -JwtSecret "my-super-secret-key-32-characters"
```

---

## Alternative: OLD Method (If You Have Permissions)

If you somehow get a personal Microsoft account with permissions:

<details>
<summary>Click to expand: Service Principal Method</summary>

```powershell
# Create service principal
az ad sp create-for-rbac `
  --name "github-actions-ctse" `
  --role Contributor `
  --scopes /subscriptions/$(az account show --query id -o tsv) `
  --sdk-auth
```

Then add the JSON output as `AZURE_CREDENTIALS` GitHub secret. But **this won't work with university accounts**.

</details>

---

## Verify Deployment

### Check Container Apps

```powershell
# List all container apps
az containerapp list `
  --resource-group ctse-microservices-rg `
  --output table

# Get API Gateway URL
az containerapp show `
  --name api-gateway `
  --resource-group ctse-microservices-rg `
  --query properties.configuration.ingress.fqdn `
  -o tsv
```

### Test Your API

```powershell
# Set your gateway URL (from command above)
$gateway = "YOUR-GATEWAY-URL.azurecontainerapps.io"

# Test health endpoint
curl "https://$gateway/health"

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

## Common Issues

### Issue: "az login" fails

**Solution**: Make sure Azure CLI is installed

```powershell
# Check Azure CLI version
az --version

# Install if missing: https://learn.microsoft.com/en-us/cli/azure/install-azure-cli-windows
```

### Issue: "Cannot pull image from ghcr.io"

**Solution**: Images must be public or you need GitHub token

```powershell
# Make repository packages public:
# GitHub → Repository → Packages → Package Settings → Change visibility → Public
```

### Issue: "GitHub Actions not building images"

**Solution**: Check the Actions tab for errors

```powershell
# View workflow runs:
# https://github.com/Y4S1-Projects/CTSE-Assignment-Microservices-Cloud-Deployments/actions
```

---

## Quick Reference

| Command                                                  | Purpose                          |
| -------------------------------------------------------- | -------------------------------- |
| `az login`                                               | Login to Azure with your account |
| `.\deploy-from-github.ps1`                               | Deploy using GitHub images       |
| `az containerapp list -g ctse-microservices-rg -o table` | List deployed apps               |
| `az group delete -n ctse-microservices-rg --yes`         | Delete everything                |

---

## Files Reference

- **[deploy-from-github.ps1](deploy-from-github.ps1)** - New deployment script (no permissions needed)
- **[deploy-azure.ps1](deploy-azure.ps1)** - Old script (builds locally, needs Docker)
- **[.github/workflows/deploy.yml](.github/workflows/deploy.yml)** - Updated to skip deployment if no AZURE_CREDENTIALS
- **[AZURE_DEPLOYMENT_GUIDE.md](AZURE_DEPLOYMENT_GUIDE.md)** - Comprehensive deployment guide
- **[DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)** - Pre/post deployment verification

---

## Summary

✅ **Perfect for University Accounts:**

1. Push code → GitHub builds images automatically
2. Run `az login` → Login with your university account
3. Run `.\deploy-from-github.ps1` → Deploy to Azure
4. Done! No service principal needed.

✅ **Benefits:**

- ✅ No elevated permissions required
- ✅ Works with university Azure accounts
- ✅ Uses pre-built images from GitHub
- ✅ Single command deployment
- ✅ Auto-generates JWT secret

🎉 **You're ready to deploy!**
