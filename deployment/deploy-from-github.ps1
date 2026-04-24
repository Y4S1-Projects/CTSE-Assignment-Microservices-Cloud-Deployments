# Deploy to Azure Container Apps from GitHub Container Registry.
# Uses the currently logged-in Azure account and defaults to low-cost demo settings.

param(
    [string]$AppPrefix = "ctse-assignment",
    [string]$ResourceGroup = "ctse-assignment",
    [string]$Location = "southeastasia",
    [string]$EnvironmentName = "ctse-assignment-env",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$JwtSecret = "",
    [string]$DatabaseUrl = "jdbc:postgresql://ep-bitter-heart-aogzv7s0.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require",
    [string]$DatabaseUser = "neondb_owner",
    [string]$DatabasePassword = "npg_EcoVLd0FiD7a",
    [string]$GitHubUsername = "",
    [string]$GitHubToken = ""
)

$ErrorActionPreference = "Stop"
$ResolvedAppNames = @{}

# In PowerShell 7+, avoid turning non-fatal native stderr (CLI warnings) into terminating errors.
if ($PSVersionTable.PSVersion.Major -ge 7) {
    $PSNativeCommandUseErrorActionPreference = $false
}

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

    $count = az containerapp list --resource-group $ResourceGroup --query "[?name=='$Name'] | length(@)" --output tsv --only-show-errors 2>$null
    if ([string]::IsNullOrWhiteSpace($count)) {
        return $false
    }

    return [int]$count -gt 0
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
    if (-not [string]::IsNullOrWhiteSpace($GitHubUsername) -and -not [string]::IsNullOrWhiteSpace($GitHubToken)) {
        return @("--registry-server", "ghcr.io", "--registry-username", $GitHubUsername, "--registry-password", $GitHubToken)
    }

    Write-Host "No GitHub registry credentials provided; deploying with public GHCR image access only." -ForegroundColor Yellow
    return @()
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
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            $fqdn = az containerapp show --name $appName --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" --output tsv --only-show-errors 2>$null
            if (-not [string]::IsNullOrWhiteSpace($fqdn)) {
                return $fqdn
            }
        } catch {
        }

        Start-Sleep -Seconds 3
    }

    return ""
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

if ([string]::IsNullOrWhiteSpace($DatabaseUrl) -or [string]::IsNullOrWhiteSpace($DatabaseUser) -or [string]::IsNullOrWhiteSpace($DatabasePassword)) {
    throw "Neon database configuration is incomplete. Pass -DatabaseUrl, -DatabaseUser and -DatabasePassword."
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

# ── 5a. Deploy auth-service first (other services depend on it) ──
$authEnvVars = @{
    JWT_SECRET                    = $JwtSecret
    DATABASE_URL                  = $DatabaseUrl
    DATABASE_USER                 = $DatabaseUser
    DATABASE_PASSWORD             = $DatabasePassword
    ADMIN_BOOTSTRAP_ENABLED       = "true"
    CUSTOMER_BOOTSTRAP_ENABLED    = "true"
    ADMIN_PASSWORD                = "Admin@12345"
    CUSTOMER_PASSWORD             = "Customer@12345"
}

Deploy-ContainerApp -ServiceName "auth-service" -Port 8081 -Ingress "internal" -EnvVars $authEnvVars

# ── 5b. Resolve internal FQDNs so remaining services can reach each other ──
$authServiceUrl    = "http://$($ResolvedAppNames['auth-service'])"
$catalogServiceUrl = "http://$($ResolvedAppNames['catalog-service'])"
$orderServiceUrl   = "http://$($ResolvedAppNames['order-service'])"
$paymentServiceUrl = "http://$($ResolvedAppNames['payment-service'])"

Write-Host ""
Write-Host "Internal service URLs:" -ForegroundColor White
Write-Host "  Auth:    $authServiceUrl" -ForegroundColor Gray
Write-Host "  Catalog: $catalogServiceUrl" -ForegroundColor Gray
Write-Host "  Order:   $orderServiceUrl" -ForegroundColor Gray
Write-Host "  Payment: $paymentServiceUrl" -ForegroundColor Gray

# ── 5c. Deploy catalog-service (needs auth) ──
$catalogEnvVars = @{
    JWT_SECRET        = $JwtSecret
    DATABASE_URL      = $DatabaseUrl
    DATABASE_USER     = $DatabaseUser
    DATABASE_PASSWORD = $DatabasePassword
    SERVICE_AUTH_URL  = $authServiceUrl
}
Deploy-ContainerApp -ServiceName "catalog-service" -Port 8082 -Ingress "internal" -EnvVars $catalogEnvVars

# ── 5d. Deploy order-service (needs auth, catalog, payment) ──
$orderEnvVars = @{
    JWT_SECRET          = $JwtSecret
    DATABASE_URL        = $DatabaseUrl
    DATABASE_USER       = $DatabaseUser
    DATABASE_PASSWORD   = $DatabasePassword
    SERVICE_AUTH_URL    = $authServiceUrl
    SERVICE_CATALOG_URL = $catalogServiceUrl
    SERVICE_PAYMENT_URL = $paymentServiceUrl
}
Deploy-ContainerApp -ServiceName "order-service" -Port 8083 -Ingress "internal" -EnvVars $orderEnvVars

# ── 5e. Deploy payment-service (needs auth, order) ──
$paymentEnvVars = @{
    JWT_SECRET        = $JwtSecret
    DATABASE_URL      = $DatabaseUrl
    DATABASE_USER     = $DatabaseUser
    DATABASE_PASSWORD = $DatabasePassword
    SERVICE_AUTH_URL  = $authServiceUrl
    SERVICE_ORDER_URL = $orderServiceUrl
}
Deploy-ContainerApp -ServiceName "payment-service" -Port 8084 -Ingress "internal" -EnvVars $paymentEnvVars

# ── 5f. Deploy api-gateway (external facing, routes to all backend services) ──
Deploy-ContainerApp -ServiceName "api-gateway" -Port 8080 -Ingress "external" -EnvVars @{
    AUTH_SERVICE_URL    = $authServiceUrl
    CATALOG_SERVICE_URL = $catalogServiceUrl
    JWT_SECRET          = $JwtSecret
    ORDER_SERVICE_URL   = $orderServiceUrl
    PAYMENT_SERVICE_URL = $paymentServiceUrl
}

$gatewayUrl = Get-AppFqdn -ServiceName "api-gateway"
if ([string]::IsNullOrWhiteSpace($gatewayUrl)) {
    throw "API gateway URL could not be resolved after deployment. Aborting frontend deployment."
}

Write-Section "Step 6: Deploy Frontend"
Deploy-ContainerApp -ServiceName "frontend" -Port 3000 -Ingress "external" -Cpu "0.25" -Memory "0.5Gi" -EnvVars @{
    NEXT_PUBLIC_API_URL     = "https://$gatewayUrl"
    NEXT_PUBLIC_API_BASE_URL = "https://$gatewayUrl"
    NODE_ENV                 = "production"
    PORT                     = "3000"
}

Show-Result
