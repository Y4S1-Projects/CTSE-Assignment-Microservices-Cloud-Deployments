# Deploy Microservices to Azure Container Apps & Frontend to App Service from GitHub Container Registry
# No service principal required. Uses the currently logged-in Azure account.

param(
    [string]$ResourceGroup = "ctse-microservices-rg",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "ctse-env",
    [string]$AppServicePlanName = "ctse-plan",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$JwtSecret = "",
    [switch]$SkipFrontend = $false
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

function Ensure-AppServicePlan {
    Write-Section "Step 3b: Ensure App Service Plan"
    if (-not $SkipFrontend) {
        $exists = az appservice plan show --name $AppServicePlanName --resource-group $ResourceGroup 2>$null
        if ($exists) {
            Write-Host "App Service Plan already exists: $AppServicePlanName" -ForegroundColor Green
        } else {
            Write-Host "Creating App Service Plan: $AppServicePlanName" -ForegroundColor Yellow
            az appservice plan create `
                --name $AppServicePlanName `
                --resource-group $ResourceGroup `
                --sku B1 `
                --is-linux `
                --location $Location `
                --output none
            Write-Host "Created App Service Plan: $AppServicePlanName" -ForegroundColor Green
        }
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

function Deploy-Frontend {
    param(
        [string]$GatewayUrl
    )

    $registry = "ghcr.io"
    $repoLower = $GitHubRepo.ToLower()
    $webAppName = "ctse-frontend"
    $image = "$registry/$repoLower/frontend`:$ImageTag"

    Write-Host ""
    Write-Host "Deploying frontend" -ForegroundColor Cyan
    Write-Host "Image: $image" -ForegroundColor Gray
    Write-Host "Web App: $webAppName" -ForegroundColor Gray

    try {
        # Check if web app already exists
        $existingApp = az webapp show --name $webAppName --resource-group $ResourceGroup --query "name" --output tsv 2>$null
        
        if ([string]::IsNullOrWhiteSpace($existingApp)) {
            # Create web app
            az webapp create `
                --resource-group $ResourceGroup `
                --plan $AppServicePlanName `
                --name $webAppName `
                --deployment-container-image-name $image `
                --output none

            Write-Host "Created: $webAppName" -ForegroundColor Green
        } else {
            Write-Host "Updating existing: $webAppName" -ForegroundColor Green
        }

        # Configure container
        az webapp config container set `
            --name $webAppName `
            --resource-group $ResourceGroup `
            --container-image-name $image `
            --container-registry-url "https://$registry" `
            --output none

        Write-Host "Configured container settings" -ForegroundColor Green

        # Set environment variables
        if (-not [string]::IsNullOrWhiteSpace($GatewayUrl)) {
            az webapp config appsettings set `
                --resource-group $ResourceGroup `
                --name $webAppName `
                --settings NEXT_PUBLIC_API_URL=$GatewayUrl NEXT_PUBLIC_API_BASE_URL=$GatewayUrl `
                --output none
            
            Write-Host "Set API Gateway environment variable" -ForegroundColor Green
        }

        # Restart to pull latest image
        az webapp restart --name $webAppName --resource-group $ResourceGroup --output none
        Write-Host "Restarted $webAppName" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Error deploying frontend: $_" -ForegroundColor Yellow
    }

    return $webAppName
}

function Show-Result {
    param(
        [string]$GatewayUrl,
        [string]$FrontendWebApp
    )

    Write-Section "Step 5: Deployment Summary"
    
    try {
        $gatewayUrl = az containerapp show --name "api-gateway" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" --output tsv 2>$null
    } catch {
        $gatewayUrl = ""
    }

    Write-Host ""
    Write-Host "Deployment complete." -ForegroundColor Green
    Write-Host ""

    # Show Gateway Info
    if (-not [string]::IsNullOrWhiteSpace($gatewayUrl)) {
        Write-Host "✅ API Gateway:" -ForegroundColor Cyan
        Write-Host "  URL: https://$gatewayUrl" -ForegroundColor Green
        Write-Host "  Health: https://$gatewayUrl/health" -ForegroundColor White
        Write-Host "  Swagger: https://$gatewayUrl/swagger-ui.html" -ForegroundColor White
        Write-Host "  Auth Docs: https://$gatewayUrl/auth/v3/api-docs" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host "⚠️  API Gateway URL not yet available" -ForegroundColor Yellow
        Write-Host "Run: az containerapp show --name api-gateway --resource-group $ResourceGroup --query properties.configuration.ingress.fqdn -o tsv" -ForegroundColor Gray
        Write-Host ""
    }

    # Show Frontend Info
    if (-not [string]::IsNullOrWhiteSpace($FrontendWebApp) -and -not $SkipFrontend) {
        try {
            $frontendUrl = az webapp show --name $FrontendWebApp --resource-group $ResourceGroup --query "defaultHostName" --output tsv 2>$null
            if (-not [string]::IsNullOrWhiteSpace($frontendUrl)) {
                Write-Host "✅ Frontend Web App:" -ForegroundColor Cyan
                Write-Host "  Name: $FrontendWebApp" -ForegroundColor White
                Write-Host "  URL: https://$frontendUrl" -ForegroundColor Green
                Write-Host ""
            }
        } catch {
            Write-Host "⚠️  Could not retrieve frontend URL" -ForegroundColor Yellow
        }
    }

    # Show Quick Test Commands
    Write-Host "Quick test commands:" -ForegroundColor Yellow
    if (-not [string]::IsNullOrWhiteSpace($gatewayUrl)) {
        Write-Host "curl https://$gatewayUrl/health" -ForegroundColor Gray
        Write-Host "POST https://$gatewayUrl/auth/register" -ForegroundColor Gray
    }
    Write-Host "(Use Swagger UI for request body testing)" -ForegroundColor Gray

    Write-Host ""
    Write-Host "Monitor apps:" -ForegroundColor Yellow
    Write-Host "az containerapp list --resource-group $ResourceGroup --output table" -ForegroundColor Gray
    if (-not $SkipFrontend) {
        Write-Host "az webapp list --resource-group $ResourceGroup --output table" -ForegroundColor Gray
    }
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
if (-not $SkipFrontend) {
    Write-Host "App Service Plan: $AppServicePlanName" -ForegroundColor White
}

Ensure-AzureLogin
Ensure-ResourceGroup
Ensure-ContainerAppEnvironment
if (-not $SkipFrontend) {
    Ensure-AppServicePlan
}

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

# Get gateway URL for frontend
$gatewayUrl = ""
try {
    $gatewayUrl = az containerapp show --name "api-gateway" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" --output tsv 2>$null
    if (-not [string]::IsNullOrWhiteSpace($gatewayUrl)) {
        $gatewayUrl = "https://$gatewayUrl"
    }
} catch {
    $gatewayUrl = ""
}

# Deploy Frontend
$frontendApp = ""
if (-not $SkipFrontend) {
    Write-Section "Step 4b: Deploy Frontend"
    $frontendApp = Deploy-Frontend -GatewayUrl $gatewayUrl
}

Show-Result -GatewayUrl $gatewayUrl -FrontendWebApp $frontendApp
