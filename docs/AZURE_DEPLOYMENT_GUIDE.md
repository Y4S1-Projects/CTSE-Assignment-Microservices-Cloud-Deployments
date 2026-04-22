# Azure Deployment Guide

This repository now defaults to a low-cost Azure demo deployment.

## What Changed

The previous deployment path kept multiple Container Apps running all the time with minimum replicas above zero. That is expensive for a student/demo subscription.

The updated deployment path now does this by default:

- deploys into the `ctse-assignment` resource group
- uses the `ctse-assignment-env` Container Apps environment
- deploys `frontend`, `api-gateway`, `auth-service`, `catalog-service`, `order-service`, and `payment-service`
- scales every app to zero when idle
- caps every app at a single replica
- uses smaller CPU and memory allocations intended for demos and API verification, not production traffic

## Recommended Manual Flow

Use this flow if your university account cannot create service principals.

```powershell
git push origin main
az login
./deployment/deploy-from-github.ps1
```

That script will create missing Azure resources and update existing ones when they are already present.

## Default Azure Names

- Resource group: `ctse-assignment`
- Container Apps environment: `ctse-assignment-env`
- App prefix: `ctse-assignment`

The default app names are:

- `ctse-assignment-frontend`
- `ctse-assignment-api-gateway`
- `ctse-assignment-auth-service`
- `ctse-assignment-catalog-service`
- `ctse-assignment-order-service`
- `ctse-assignment-payment-service`

## Manual Deployment Commands

```powershell
# Authenticate to Azure
az login

# Deploy or update the full demo stack
./deployment/deploy-from-github.ps1
```

Optional parameters:

```powershell
./deployment/deploy-from-github.ps1 `
  -AppPrefix "ctse-assignment" `
  -ResourceGroup "ctse-assignment" `
  -EnvironmentName "ctse-assignment-env" `
  -Location "eastus" `
  -ImageTag "latest" `
  -DatabaseUrl "jdbc:postgresql://ep-bitter-heart-aogzv7s0.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require" `
  -DatabaseUser "neondb_owner" `
  -DatabasePassword "<your-neon-db-password>"
```

## GitHub Actions Flow

When code is pushed to `main`, the workflow now:

- builds and tests the Java services
- builds the Next.js frontend
- pushes all six images to GitHub Container Registry
- deploys with the same low-cost settings when `AZURE_CREDENTIALS` is configured

If `AZURE_CREDENTIALS` is not configured, the pipeline still builds the images and prints the manual deployment command.

For successful hosted deployment, also configure these secrets:

- `JWT_SECRET`
- `NEON_DATABASE_URL`
- `NEON_DATABASE_USER`
- `NEON_DATABASE_PASSWORD`

## Verification

Get the public URLs:

```powershell
az containerapp show --name ctse-assignment-api-gateway --resource-group ctse-assignment --query properties.configuration.ingress.fqdn -o tsv
az containerapp show --name ctse-assignment-frontend --resource-group ctse-assignment --query properties.configuration.ingress.fqdn -o tsv
```

Smoke test the gateway:

```powershell
$gateway = "<gateway-url>.azurecontainerapps.io"

curl "https://$gateway/health"
curl "https://$gateway/auth/health"
```

Open in browser:

- `https://<frontend-url>/`
- `https://<gateway-url>/swagger-ui.html`
- `https://<gateway-url>/auth/v3/api-docs`

## Cost Notes

These settings are meant to reduce consumption, not eliminate it entirely.

- Container Apps can still consume credits when requests wake the apps up.
- Cold starts are expected because min replicas are set to zero.
- Delete the resource group after the demo if you no longer need it.

## Troubleshooting

### Error: Please run `az login`

```powershell
az login
./deployment/deploy-from-github.ps1
```

### University account cannot create service principal

That is normal in many student tenants. The manual `az login` flow does not require a service principal.

### Need to inspect logs

```powershell
az containerapp logs show --name ctse-assignment-api-gateway --resource-group ctse-assignment --follow
```

### Need to inspect current scale state

```powershell
az containerapp list --resource-group ctse-assignment --output table
```

## Cleanup

Delete everything after the demonstration to stop further credit use.

```powershell
az group delete --name ctse-assignment --yes --no-wait
```
