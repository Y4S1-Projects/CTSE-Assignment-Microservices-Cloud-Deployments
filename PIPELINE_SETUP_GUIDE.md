# ⚙️ CI/CD Pipeline Setup Guide

## Overview

This guide walks you through setting up the automated CI/CD pipeline to:

1. Automatically build, test, and scan your code
2. Build Docker images
3. Deploy to Azure Container Apps
4. Run smoke tests

---

## 📋 Prerequisites

- ✅ GitHub repository access
- ✅ Azure subscription
- ✅ Code pushed to GitHub (main/develop branch)

---

## 🔐 Step 1: Create Azure Service Principal

This allows GitHub Actions to deploy to Azure.

### Option A: Using Azure CLI (Recommended)

```bash
# Login to Azure
az login

# Get your subscription ID
az account show --query id -o tsv
# Output: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

# Create service principal
az ad sp create-for-rbac \
  --name "ctse-github-actions" \
  --role contributor \
  --scopes "/subscriptions/{SUBSCRIPTION_ID}"

# Save the full output (you'll need it in next step)
```

**Output will look like:**

```json
{
	"clientId": "00000000-0000-0000-0000-000000000000",
	"clientSecret": "aaaaaaa~bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
	"subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
	"tenantId": "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"
}
```

### Option B: Using Azure Portal

1. Go to [Azure Portal](https://portal.azure.com)
2. Search for "App registrations"
3. Click "New registration"
4. Name: `ctse-github-actions`
5. Click "Certificates & secrets"
6. Click "New client secret"
7. Copy the secret value
8. Go to "Subscriptions" → Select your subscription
9. Click "Access control (IAM)" → "Add" → "Add role assignment"
10. Role: "Contributor", Assign to the app you created

---

## 🔑 Step 2: Add GitHub Secret

This secret allows GitHub Actions to authenticate to Azure.

### Steps:

1. Go to your GitHub repository
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. **Name:** `AZURE_CREDENTIALS`
5. **Value:** Paste the entire JSON from Step 1:

```json
{
	"clientId": "00000000-0000-0000-0000-000000000000",
	"clientSecret": "aaaaaaa~bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
	"subscriptionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
	"tenantId": "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz"
}
```

6. Click **Add secret**

**Verify:** You should see `AZURE_CREDENTIALS` in the list

---

## 🧪 Step 3: Test the Pipeline

### Option A: Automatic Trigger

Push code to main or develop branch:

```bash
git add .
git commit -m "Setup CI/CD pipeline"
git push origin main
```

The pipeline will automatically start!

### Option B: Manual Trigger (if configured)

In GitHub repo → Actions → Select workflow → "Run workflow"

---

## 📊 Step 4: Monitor Pipeline Execution

### Watch Real-Time

1. Go to **GitHub Repo → Actions**
2. Click on the latest workflow run
3. Watch stages execute in real-time

**You'll see:**

```
✅ Checkout
✅ Setup Java
✅ Build Services
✅ Run Tests
✅ Security Scan
✅ Build Docker
✅ Deploy to Azure
✅ Smoke Test
✅ Summary Report
```

### View Logs

Click on any stage to see detailed logs:

```bash
$ mvn clean package -DskipTests --batch-mode
[INFO] Scanning for projects...
[INFO] Building api-gateway 1.0.0
[INFO] ... [build process logs] ...
[INFO] BUILD SUCCESS
```

### Handle Failures

If a stage fails:

1. Click on the failed stage
2. Click on the failed step
3. Read the error message
4. Fix the issue
5. Commit and push again

---

## 🔄 Step 5: Configure Azure Resources (Optional)

The pipeline can automatically create Azure resources if needed.

### Automatic Setup (Recommended)

The pipeline is configured to:

1. Check if resources exist
2. Create them if missing
3. Deploy services to Container Apps

### Manual Setup (Optional)

If you prefer to create resources first:

```bash
# Create resource group
az group create --name ctse-microservices-rg --location eastus

# Create container registry
az acr create --name ctsereg2026 --resource-group ctse-microservices-rg --sku Basic

# Create container apps environment
az containerapp env create --name ctse-env --resource-group ctse-microservices-rg --location eastus
```

---

## 📈 Step 6: Verify Deployment

Once the pipeline completes:

```bash
# List all container apps
az containerapp list --resource-group ctse-microservices-rg --output table

# Get API Gateway URL
az containerapp show --name api-gateway --resource-group ctse-microservices-rg --query "properties.configuration.ingress.fqdn"

# Test the endpoint
curl https://{API_GATEWAY_URL}/actuator/health
```

---

## 🔐 Additional Secrets (Optional)

For enhanced monitoring and security, you can add:

### For Trivy Security Scanning

Nothing needed - runs by default

### For Snyk Scanning

```plaintext
Secret Name: SNYK_TOKEN
Value: Your Snyk API token from https://app.snyk.io/account/settings
```

### For SonarCloud Analysis

```plaintext
Secret Name: SONAR_TOKEN
Value: Your SonarCloud token

Secret Name: SONAR_HOST_URL
Value: https://sonarcloud.io

Secret Name: SONAR_PROJECT_KEY
Value: Your project key

Secret Name: SONAR_ORGANIZATION
Value: Your organization key
```

---

## 🚀 Complete Setup Checklist

- [ ] Azure subscription created
- [ ] Service principal created with `az ad sp create-for-rbac`
- [ ] `AZURE_CREDENTIALS` secret added to GitHub
- [ ] Code committed to main/develop branch
- [ ] GitHub Actions workflow triggered
- [ ] Pipeline executed successfully
- [ ] Services deployed to Azure Container Apps
- [ ] Health endpoints tested and working
- [ ] Logs accessible from GitHub Actions UI

---

## 🔍 Verify Each Step

### Step 1: Azure Service Principal

```bash
# Verify it was created
az ad sp list --display-name "ctse-github-actions"
```

### Step 2: GitHub Secret

```bash
# In GitHub UI:
Settings → Secrets → AZURE_CREDENTIALS (should be listed)
```

### Step 3: Pipeline Execution

```
GitHub Actions → Actions tab → See workflow runs
```

### Step 4: Services Deployed

```bash
# Check Azure
az containerapp list --resource-group ctse-microservices-rg
```

### Step 5: Services Running

```bash
# Test the API
curl https://$API_GATEWAY_URL/actuator/health
# Should return: {"status":"UP"}
```

---

## ❌ Troubleshooting

### Issue: "Azure authentication failed"

**Error message:** `Error: Authentication failed`

**Solutions:**

1. Verify `AZURE_CREDENTIALS` secret is set correctly
2. Check service principal was created successfully: `az ad sp list`
3. Verify service principal has Contributor role
4. Subscription ID in credentials matches: `az account show`

**Test:**

```bash
az login --service-principal \
  -u "{clientId}" \
  -p "{clientSecret}" \
  --tenant "{tenantId}"
```

### Issue: "Docker build failed"

**Error message:** `Error: docker build failed exit code 1`

**Solutions:**

1. Check Dockerfile syntax in each service
2. Verify Maven builds locally: `mvn clean package -DskipTests`
3. Check `.dockerignore` files don't exclude necessary files
4. Look at full build logs in GitHub Actions

### Issue: "Deployment timed out"

**Error message:** `Timeout waiting for deployment`

**Solutions:**

1. Check Azure Container Apps quota
2. Verify resource group exists
3. Check Azure region availability for Container Apps
4. Increase timeout in workflow file if needed

### Issue: "Tests failing"

**Error message:** Tests fail during pipeline but pass locally

**Solutions:**

1. Run locally: `mvn test`
2. Check for environment-specific issues
3. Add environment variables to GitHub Actions if needed
4. Check log output for assertion failures

---

## 📚 Workflow File Location

**File:** `.github/workflows/deploy.yml`

**Key configurations:**

```yaml
# Triggers
on:
  push:
    branches: ["main", "develop"]

# Where to run
runs-on: ubuntu-latest

# Permission scopes
permissions:
  contents: read
  packages: write

# Environment variables
env:
  AZURE_RESOURCE_GROUP: ctse-microservices-rg
  AZURE_LOCATION: eastus
```

---

## 🔄 How to Update the Pipeline

### Edit the workflow file:

1. Go to `.github/workflows/deploy.yml`
2. Make changes
3. Commit and push
4. Changes take effect on next push

### Common changes:

**Add a new build step:**

```yaml
- name: My Custom Step
  run: echo "Custom command"
```

**Change deployment region:**

```yaml
env:
  AZURE_LOCATION: westus
```

**Disable a stage:**

```yaml
if: false # Add to any job to disable it
```

---

## 📊 Pipeline Costs

### Azure Container Registry: ~$5/month

```bash
# Free tier
- 2M builds/month free
- $0.10 per 1000 additional builds
```

### Azure Container Apps: ~$20-50/month

```
- Per vCPU/hour: ~$0.05
- Per GB memory/hour: ~$0.013
- 5 services × $3-10/month each
```

### Total with free tier: **FREE** (Azure Student credit)

---

## 🎓 Learning Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Azure Container Apps Guide](https://learn.microsoft.com/en-us/azure/container-apps/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Maven Documentation](https://maven.apache.org/)

---

## 🔐 Security Best Practices

✅ **Do:**

- Use service principals (don't use personal accounts)
- Store secrets in GitHub Secrets (not in code)
- Use minimal permissions (Contributor role)
- Rotate credentials regularly
- Enable branch protection rules

❌ **Don't:**

- Commit secrets to git
- Use personal Azure accounts
- Grant Owner level permissions
- Share service principal credentials
- Use the same credentials for multiple projects

---

## ✅ You're Ready!

Once you've completed these steps:

1. ✅ Service principal created
2. ✅ GitHub secret added
3. ✅ Code pushed to main
4. ✅ Pipeline executed
5. ✅ Services deployed

**Your CI/CD pipeline is fully operational!** 🎉

---

## 📞 Next Steps

1. **Monitor First Deployment:**
   - Go to Actions tab
   - Watch workflow execute
   - Check Azure Container Apps

2. **Test Deployed Services:**
   - Get API Gateway URL from Azure
   - Test health endpoint
   - Access Swagger docs

3. **Configure Monitoring (Optional):**
   - Setup Azure Monitor alerts
   - Add logging to services
   - Setup email notifications

4. **Make Code Changes:**
   - Push to main branch
   - Pipeline runs automatically
   - Services update automatically

---

**Status:** ✅ Pipeline Ready  
**Setup Time:** ~10 minutes  
**Next:** Push code to main branch and watch it deploy automatically!
