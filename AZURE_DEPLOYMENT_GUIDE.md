# Azure Deployment Guide

This is the single deployment guide for this repository.

## Recommended Flow (No Elevated Permissions)

Use this flow if your university account cannot create service principals.

1. Push code to `main` (GitHub Actions builds and publishes images to `ghcr.io`)
2. Login with your Azure account
3. Deploy using the repository script

```powershell
git push origin main
az login
./deploy-from-github.ps1
```

## How the Pipeline Works

When you push to `main`:

- `build-and-test`: builds all services
- `security-scan`: runs Trivy scan
- `build-docker`: builds and pushes images to GitHub Container Registry
- `deploy-to-azure`: runs only if `AZURE_CREDENTIALS` exists
- `manual-deployment-notice`: shows manual deploy guidance when `AZURE_CREDENTIALS` is not set

For university accounts, the normal path is:

- let GitHub Actions build images
- run `./deploy-from-github.ps1` manually

## Scripts

- `deploy-from-github.ps1` (preferred): deploys from `ghcr.io` images, no service principal required
- `deploy-azure.ps1` (alternative): local Docker/ACR based deployment

## Required Tools

- Azure CLI (`az`)
- PowerShell 5.1+
- GitHub repository push access

## Optional GitHub Secrets

These are only required if you want full auto-deploy from GitHub Actions:

- `AZURE_CREDENTIALS`: service principal JSON
- `JWT_SECRET`: JWT signing key

If `AZURE_CREDENTIALS` is missing, auto-deploy is skipped and manual deploy is expected.

## Manual Deployment Commands

```powershell
# 1) Authenticate
az login

# 2) Deploy all services from ghcr images
./deploy-from-github.ps1

# 3) Get gateway URL
az containerapp show --name api-gateway --resource-group ctse-microservices-rg --query properties.configuration.ingress.fqdn -o tsv
```

## Post-Deployment Verification

```powershell
# Replace with your URL
$gateway = "<gateway-url>.azurecontainerapps.io"

curl "https://$gateway/health"
curl "https://$gateway/auth/health"
```

Open in browser:

- `https://<gateway-url>/swagger-ui.html`
- `https://<gateway-url>/auth/v3/api-docs`

## Troubleshooting

### Error: Please run 'az login'

Run:

```powershell
az login
```

Then rerun:

```powershell
./deploy-from-github.ps1
```

### University account cannot create service principal

That is expected in many managed tenants. Use the recommended manual flow; no service principal is required.

### Container app does not update

Check logs:

```powershell
az containerapp logs show --name api-gateway --resource-group ctse-microservices-rg --follow
```

List all apps:

```powershell
az containerapp list --resource-group ctse-microservices-rg --output table
```

## Cleanup

```powershell
az group delete --name ctse-microservices-rg --yes --no-wait
```
