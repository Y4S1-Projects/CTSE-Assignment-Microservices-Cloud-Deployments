# Deploy to Azure Container Apps from GitHub Container Registry.
# Uses the currently logged-in Azure account and defaults to low-cost demo settings.

param(
    [string]$AppPrefix = "ctse-assignment",
    [string]$ResourceGroup = "ctse-assignment",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "ctse-assignment-env",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$JwtSecret = "",
    [string]$MongoDbUri = "",
    [string]$GitHubUsername = "",
    [string]$GitHubToken = ""
)

$ErrorActionPreference = "Stop"
$ResolvedAppNames = @{}

function Write-Section {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Get-PreferredAppName {
    param([string]$ServiceName)

    if ([string]::IsNullOrWhiteSpace($AppPrefix)) {
        return $ServiceName
    }

    return "$AppPrefix-$ServiceName"
}

function Test-ContainerAppExists {
    param([string]$Name)

    & az containerapp show --name $Name --resource-group $ResourceGroup --output none 2>$null | Out-Null
    return $LASTEXITCODE -eq 0
}

function Resolve-AppName {
    param([string]$ServiceName)

    $preferredName = Get-PreferredAppName -ServiceName $ServiceName
    if (Test-ContainerAppExists -Name $preferredName) {
        return $preferredName
    }

    if ($preferredName -ne $ServiceName -and (Test-ContainerAppExists -Name $ServiceName)) {
        Write-Host "Reusing existing legacy app name: $ServiceName" -ForegroundColor Yellow
        return $ServiceName
    }

    return $preferredName
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

function Ensure-ContainerAppCli {
    Write-Section "Step 2: Ensure Azure CLI Support"
    az extension add --name containerapp --upgrade --only-show-errors | Out-Null
    az provider register --namespace Microsoft.App --wait --only-show-errors | Out-Null
    az provider register --namespace Microsoft.OperationalInsights --wait --only-show-errors | Out-Null
    Write-Host "Azure Container Apps tooling is ready." -ForegroundColor Green
}

function Ensure-ResourceGroup {
    Write-Section "Step 3: Ensure Resource Group"
    $exists = az group exists --name $ResourceGroup
    if ($exists -eq "true") {
        Write-Host "Resource group already exists: $ResourceGroup" -ForegroundColor Green
    } else {
        az group create --name $ResourceGroup --location $Location --tags project=$AppPrefix purpose=demo costProfile=free-tier --output none
        Write-Host "Created resource group: $ResourceGroup" -ForegroundColor Green
    }
}

function Ensure-ContainerAppEnvironment {
    Write-Section "Step 4: Ensure Container Apps Environment"
    $count = az containerapp env list --resource-group $ResourceGroup --query "[?name=='$EnvironmentName'] | length(@)" --output tsv
    if ([int]$count -gt 0) {
        Write-Host "Environment already exists: $EnvironmentName" -ForegroundColor Green
    } else {
        az containerapp env create --name $EnvironmentName --resource-group $ResourceGroup --location $Location --tags project=$AppPrefix purpose=demo costProfile=free-tier --output none
        Write-Host "Created environment: $EnvironmentName" -ForegroundColor Green
    }
}

function Build-EnvVarArgs {
    param([hashtable]$EnvVars)

    $result = @()
    foreach ($key in ($EnvVars.Keys | Sort-Object)) {
        $result += "$key=$($EnvVars[$key])"
    }

    return $result
}

function Get-RegistryArgs {
    $args = @("--registry-server", "ghcr.io")
    if (-not [string]::IsNullOrWhiteSpace($GitHubUsername) -and -not [string]::IsNullOrWhiteSpace($GitHubToken)) {
        $args += @("--registry-username", $GitHubUsername, "--registry-password", $GitHubToken)
    }

    return $args
}

function Deploy-ContainerApp {
    param(
        [string]$ServiceName,
        [int]$Port,
        [string]$Ingress,
        [hashtable]$EnvVars = @{},
        [string]$Cpu = "0.25",
        [string]$Memory = "0.5Gi"
    )

    $appName = $ResolvedAppNames[$ServiceName]
    $registry = "ghcr.io"
    $repoLower = $GitHubRepo.ToLower()
    $image = "$registry/$repoLower/$ServiceName`:$ImageTag"
    $envVarArgs = Build-EnvVarArgs -EnvVars $EnvVars
    $registryArgs = Get-RegistryArgs

    Write-Host ""
    Write-Host "Deploying $ServiceName as $appName" -ForegroundColor Cyan
    Write-Host "Image: $image" -ForegroundColor Gray
    Write-Host "Scale: min 0 / max 1, CPU $Cpu, Memory $Memory" -ForegroundColor DarkGray

    $createArgs = @(
        "containerapp", "create",
        "--name", $appName,
        "--resource-group", $ResourceGroup,
        "--environment", $EnvironmentName,
        "--image", $image,
        "--target-port", "$Port",
        "--ingress", $Ingress,
        "--cpu", $Cpu,
        "--memory", $Memory,
        "--min-replicas", "0",
        "--max-replicas", "1",
        "--tags", "project=$AppPrefix", "purpose=demo", "costProfile=free-tier",
        "--output", "none"
    ) + $registryArgs

    if ($envVarArgs.Count -gt 0) {
        $createArgs += "--env-vars"
        $createArgs += $envVarArgs
    }

    if (Test-ContainerAppExists -Name $appName) {
        $updateArgs = @(
            "containerapp", "update",
            "--name", $appName,
            "--resource-group", $ResourceGroup,
            "--image", $image,
            "--cpu", $Cpu,
            "--memory", $Memory,
            "--min-replicas", "0",
            "--max-replicas", "1",
            "--output", "none"
        )

        if ($envVarArgs.Count -gt 0) {
            $updateArgs += "--set-env-vars"
            $updateArgs += $envVarArgs
        }

        & az @updateArgs
        Write-Host "Updated: $appName" -ForegroundColor Green
        return
    }

    & az @createArgs
    Write-Host "Created: $appName" -ForegroundColor Green
}

function Get-AppFqdn {
    param([string]$ServiceName)

    $appName = $ResolvedAppNames[$ServiceName]
    try {
        return az containerapp show --name $appName --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" --output tsv 2>$null
    } catch {
        return ""
    }
}

function Show-Result {
    Write-Section "Step 7: Deployment Summary"

    $gatewayUrl = Get-AppFqdn -ServiceName "api-gateway"
    $frontendUrl = Get-AppFqdn -ServiceName "frontend"

    Write-Host ""
    Write-Host "Deployment complete." -ForegroundColor Green
    Write-Host ""

    if (-not [string]::IsNullOrWhiteSpace($frontendUrl)) {
        Write-Host "Frontend URL: https://$frontendUrl" -ForegroundColor Cyan
    }

    if (-not [string]::IsNullOrWhiteSpace($gatewayUrl)) {
        Write-Host "Gateway URL: https://$gatewayUrl" -ForegroundColor Cyan
        Write-Host "Health: https://$gatewayUrl/health" -ForegroundColor White
        Write-Host "Swagger: https://$gatewayUrl/swagger-ui.html" -ForegroundColor White
        Write-Host "Auth Docs: https://$gatewayUrl/auth/v3/api-docs" -ForegroundColor White
    } else {
        Write-Host "Could not resolve the gateway URL yet." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Cost profile applied:" -ForegroundColor Yellow
    Write-Host "- Consumption-based Azure Container Apps only" -ForegroundColor Gray
    Write-Host "- All apps scale to zero when idle" -ForegroundColor Gray
    Write-Host "- Max replicas capped at 1 for demo traffic" -ForegroundColor Gray
    Write-Host "- Small CPU and memory allocations" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Monitor apps:" -ForegroundColor Yellow
    Write-Host "az containerapp list --resource-group $ResourceGroup --output table" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Delete all resources when the demo is over:" -ForegroundColor Yellow
    Write-Host "az group delete --name $ResourceGroup --yes --no-wait" -ForegroundColor Gray
}

if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    $JwtSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object { [char]$_ })
    Write-Host "Generated JWT_SECRET: $JwtSecret" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Deployment parameters:" -ForegroundColor Yellow
Write-Host "App Prefix: $AppPrefix" -ForegroundColor White
Write-Host "Resource Group: $ResourceGroup" -ForegroundColor White
Write-Host "Location: $Location" -ForegroundColor White
Write-Host "Environment: $EnvironmentName" -ForegroundColor White
Write-Host "Repository: $GitHubRepo" -ForegroundColor White
Write-Host "Image Tag: $ImageTag" -ForegroundColor White

if ([string]::IsNullOrWhiteSpace($MongoDbUri)) {
    Write-Host "MongoDbUri not provided. auth-service will use its internal default URI." -ForegroundColor Yellow
    Write-Host "For cloud deployments, pass -MongoDbUri with a reachable MongoDB connection string." -ForegroundColor Yellow
}

Ensure-AzureLogin
Ensure-ContainerAppCli
Ensure-ResourceGroup
Ensure-ContainerAppEnvironment

$ResolvedAppNames["auth-service"] = Resolve-AppName -ServiceName "auth-service"
$ResolvedAppNames["catalog-service"] = Resolve-AppName -ServiceName "catalog-service"
$ResolvedAppNames["order-service"] = Resolve-AppName -ServiceName "order-service"
$ResolvedAppNames["payment-service"] = Resolve-AppName -ServiceName "payment-service"
$ResolvedAppNames["api-gateway"] = Resolve-AppName -ServiceName "api-gateway"
$ResolvedAppNames["frontend"] = Resolve-AppName -ServiceName "frontend"

Write-Host "Resolved App Names:" -ForegroundColor White
Write-Host "  Frontend: $($ResolvedAppNames['frontend'])" -ForegroundColor Gray
Write-Host "  Gateway: $($ResolvedAppNames['api-gateway'])" -ForegroundColor Gray
Write-Host "  Auth: $($ResolvedAppNames['auth-service'])" -ForegroundColor Gray
Write-Host "  Catalog: $($ResolvedAppNames['catalog-service'])" -ForegroundColor Gray
Write-Host "  Order: $($ResolvedAppNames['order-service'])" -ForegroundColor Gray
Write-Host "  Payment: $($ResolvedAppNames['payment-service'])" -ForegroundColor Gray

Write-Section "Step 5: Deploy Backend Services"

$authEnvVars = @{ JWT_SECRET = $JwtSecret }
if (-not [string]::IsNullOrWhiteSpace($MongoDbUri)) {
    $authEnvVars["MONGODB_URI"] = $MongoDbUri
}

Deploy-ContainerApp -ServiceName "auth-service" -Port 8081 -Ingress "internal" -EnvVars $authEnvVars
Deploy-ContainerApp -ServiceName "catalog-service" -Port 8082 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }
Deploy-ContainerApp -ServiceName "order-service" -Port 8083 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }
Deploy-ContainerApp -ServiceName "payment-service" -Port 8084 -Ingress "internal" -EnvVars @{ JWT_SECRET = $JwtSecret }

$authServiceUrl = "http://$($ResolvedAppNames['auth-service'])"
$catalogServiceUrl = "http://$($ResolvedAppNames['catalog-service'])"
$orderServiceUrl = "http://$($ResolvedAppNames['order-service'])"
$paymentServiceUrl = "http://$($ResolvedAppNames['payment-service'])"

Deploy-ContainerApp -ServiceName "api-gateway" -Port 8080 -Ingress "external" -EnvVars @{
    AUTH_SERVICE_URL = $authServiceUrl
    CATALOG_SERVICE_URL = $catalogServiceUrl
    JWT_SECRET = $JwtSecret
    ORDER_SERVICE_URL = $orderServiceUrl
    PAYMENT_SERVICE_URL = $paymentServiceUrl
}

$gatewayUrl = Get-AppFqdn -ServiceName "api-gateway"
if ([string]::IsNullOrWhiteSpace($gatewayUrl)) {
    throw "API gateway URL could not be resolved after deployment. Aborting frontend deployment."
}

Write-Section "Step 6: Deploy Frontend"
Deploy-ContainerApp -ServiceName "frontend" -Port 3000 -Ingress "external" -Cpu "0.25" -Memory "0.5Gi" -EnvVars @{
    NEXT_PUBLIC_API_BASE_URL = "https://$gatewayUrl"
    NODE_ENV = "production"
    PORT = "3000"
}

Show-Result
