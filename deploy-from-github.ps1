# Deploy to Azure Container Apps from GitHub Container Registry
# No service principal required. Uses the currently logged-in Azure account.

param(
    [string]$ResourceGroup = "ctse-microservices-rg",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "ctse-env",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$JwtSecret = ""
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Ensure-AzureLogin {
    Write-Section "Step 1: Check Azure Authentication"
    try {
        $account = az account show --output json 2>$null | ConvertFrom-Json
        if ($null -eq $account) {
            throw "No active Azure session"
        }
        Write-Host "Logged in as: $($account.user.name)" -ForegroundColor Green
        Write-Host "Subscription: $($account.name)" -ForegroundColor Gray
    } catch {
        throw "Not logged in to Azure. Run 'az login' and re-run this script."
    }
}

function Ensure-ResourceGroup {
    Write-Section "Step 2: Ensure Resource Group"
    $exists = az group exists --name $ResourceGroup
    if ($exists -eq "true") {
        Write-Host "Resource group already exists: $ResourceGroup" -ForegroundColor Green
    } else {
        az group create --name $ResourceGroup --location $Location --output none
        Write-Host "Created resource group: $ResourceGroup" -ForegroundColor Green
    }
}

function Ensure-ContainerAppEnvironment {
    Write-Section "Step 3: Ensure Container Apps Environment"
    $count = az containerapp env list --resource-group $ResourceGroup --query "[?name=='$EnvironmentName'] | length(@)" --output tsv
    if ([int]$count -gt 0) {
        Write-Host "Environment already exists: $EnvironmentName" -ForegroundColor Green
    } else {
        az containerapp env create --name $EnvironmentName --resource-group $ResourceGroup --location $Location --output none
        Write-Host "Created environment: $EnvironmentName" -ForegroundColor Green
    }
}

function Build-EnvVarArgs {
    param([hashtable]$EnvVars)

    $result = @()
    foreach ($key in $EnvVars.Keys) {
        $result += "$key=$($EnvVars[$key])"
    }
    return $result
}

function Deploy-ContainerApp {
    param(
        [string]$ServiceName,
        [int]$Port,
        [string]$Ingress,
        [hashtable]$EnvVars = @{}
    )

    $registry = "ghcr.io"
    $repoLower = $GitHubRepo.ToLower()
    $image = "$registry/$repoLower/$ServiceName`:$ImageTag"

    Write-Host ""
    Write-Host "Deploying $ServiceName" -ForegroundColor Cyan
    Write-Host "Image: $image" -ForegroundColor Gray

    $envVarArgs = Build-EnvVarArgs -EnvVars $EnvVars

    $createArgs = @(
        "containerapp", "create",
        "--name", $ServiceName,
        "--resource-group", $ResourceGroup,
        "--environment", $EnvironmentName,
        "--image", $image,
        "--target-port", "$Port",
        "--ingress", $Ingress,
        "--registry-server", $registry,
        "--cpu", "0.5",
        "--memory", "1.0Gi",
        "--min-replicas", "1",
        "--max-replicas", "3",
        "--output", "none"
    )

    if ($envVarArgs.Count -gt 0) {
        $createArgs += "--env-vars"
        $createArgs += $envVarArgs
    }

    $created = $true
    try {
        & az @createArgs 2>$null
        Write-Host "Created: $ServiceName" -ForegroundColor Green
    } catch {
        $created = $false
    }

    if (-not $created) {
        $updateArgs = @(
            "containerapp", "update",
            "--name", $ServiceName,
            "--resource-group", $ResourceGroup,
            "--image", $image,
            "--output", "none"
        )

        if ($envVarArgs.Count -gt 0) {
            $updateArgs += "--set-env-vars"
            $updateArgs += $envVarArgs
        }

        & az @updateArgs
        Write-Host "Updated: $ServiceName" -ForegroundColor Green
    }
}

function Show-Result {
    Write-Section "Step 5: Get API Gateway URL"
    try {
        $gatewayUrl = az containerapp show --name "api-gateway" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" --output tsv 2>$null
    } catch {
        $gatewayUrl = ""
    }

    Write-Host ""
    Write-Host "Deployment complete." -ForegroundColor Green
    Write-Host ""

    if (-not [string]::IsNullOrWhiteSpace($gatewayUrl)) {
        Write-Host "Gateway URL: https://$gatewayUrl" -ForegroundColor Cyan
        Write-Host "Health: https://$gatewayUrl/health" -ForegroundColor White
        Write-Host "Swagger: https://$gatewayUrl/swagger-ui.html" -ForegroundColor White
        Write-Host "Auth Docs: https://$gatewayUrl/auth/v3/api-docs" -ForegroundColor White
        Write-Host ""
        Write-Host "Quick test commands:" -ForegroundColor Yellow
        Write-Host "curl https://$gatewayUrl/health" -ForegroundColor Gray
        Write-Host "POST https://$gatewayUrl/auth/register" -ForegroundColor Gray
        Write-Host "(Use Swagger UI for request body testing)" -ForegroundColor Gray
    } else {
        Write-Host "Could not resolve gateway URL yet. Check Azure portal or run:" -ForegroundColor Yellow
        Write-Host "az containerapp show --name api-gateway --resource-group $ResourceGroup --query properties.configuration.ingress.fqdn -o tsv" -ForegroundColor Gray
    }

    Write-Host ""
    Write-Host "Monitor apps:" -ForegroundColor Yellow
    Write-Host "az containerapp list --resource-group $ResourceGroup --output table" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Delete all resources:" -ForegroundColor Yellow
    Write-Host "az group delete --name $ResourceGroup --yes --no-wait" -ForegroundColor Gray
}

if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $JwtSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
    Write-Host "Generated JWT_SECRET: $JwtSecret" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Deployment parameters:" -ForegroundColor Yellow
Write-Host "Resource Group: $ResourceGroup" -ForegroundColor White
Write-Host "Location: $Location" -ForegroundColor White
Write-Host "Environment: $EnvironmentName" -ForegroundColor White
Write-Host "Repository: $GitHubRepo" -ForegroundColor White
Write-Host "Image Tag: $ImageTag" -ForegroundColor White

Ensure-AzureLogin
Ensure-ResourceGroup
Ensure-ContainerAppEnvironment

Write-Section "Step 4: Deploy Services"

Deploy-ContainerApp -ServiceName "auth-service" -Port 8081 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }
Deploy-ContainerApp -ServiceName "catalog-service" -Port 8082 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }
Deploy-ContainerApp -ServiceName "order-service" -Port 8083 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }
Deploy-ContainerApp -ServiceName "payment-service" -Port 8084 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }

Deploy-ContainerApp -ServiceName "api-gateway" -Port 8080 -Ingress "external" -EnvVars @{
    JWT_SECRET = $JwtSecret
    AUTH_SERVICE_URL = "http://auth-service"
    CATALOG_SERVICE_URL = "http://catalog-service"
    ORDER_SERVICE_URL = "http://order-service"
    PAYMENT_SERVICE_URL = "http://payment-service"
}

Show-Result
