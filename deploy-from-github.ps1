# Deploy to Azure Container Apps from GitHub Container Registry
# No service principal required - uses your own Azure login
# Perfect for university accounts without elevated permissions

param(
    [string]$ResourceGroup = "ctse-microservices-rg",
    [string]$Location = "eastus",
    [string]$EnvironmentName = "ctse-env",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$JwtSecret = ""
)

$ErrorActionPreference = "Stop"

# ============================================================================
# BANNER
# ============================================================================
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Azure Container Apps - Manual Deployment (No Permissions)    ║" -ForegroundColor Cyan
Write-Host "║  Deploy from GitHub Container Registry                         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# JWT SECRET
# ============================================================================
if ([string]::IsNullOrWhiteSpace($JwtSecret)) {
    Write-Host "⚠️  JWT_SECRET not provided. Generating random secret..." -ForegroundColor Yellow
    $JwtSecret = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    Write-Host "✅ Generated JWT_SECRET: $JwtSecret" -ForegroundColor Green
    Write-Host "   (Save this for future deployments)`n" -ForegroundColor Gray
}

# ============================================================================
# CONFIGURATION
# ============================================================================
$Registry = "ghcr.io"
$RepoLower = $GitHubRepo.ToLower()

Write-Host "📦 Deployment Configuration:" -ForegroundColor Yellow
Write-Host "  Resource Group: $ResourceGroup" -ForegroundColor White
Write-Host "  Location: $Location" -ForegroundColor White
Write-Host "  Environment: $EnvironmentName" -ForegroundColor White
Write-Host "  Registry: $Registry" -ForegroundColor White
Write-Host "  Repository: $RepoLower" -ForegroundColor White
Write-Host "  Image Tag: $ImageTag" -ForegroundColor White
Write-Host ""

# ============================================================================
# STEP 1: CHECK AZURE LOGIN
# ============================================================================
Write-Host "🔐 Step 1: Checking Azure Authentication..." -ForegroundColor Cyan
try {
    $account = az account show 2>$null | ConvertFrom-Json
    if ($null -eq $account) {
        throw "Not logged in"
    }
    Write-Host "✅ Logged in as: $($account.user.name)" -ForegroundColor Green
    Write-Host "   Subscription: $($account.name)" -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Not logged in to Azure" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run: az login" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# ============================================================================
# STEP 2: CREATE RESOURCE GROUP
# ============================================================================
Write-Host "📁 Step 2: Ensuring Resource Group exists..." -ForegroundColor Cyan
$rgExists = az group exists --name $ResourceGroup 2>$null
if ($rgExists -eq "true") {
    Write-Host "✅ Resource group '$ResourceGroup' already exists`n" -ForegroundColor Green
} else {
    Write-Host "Creating resource group '$ResourceGroup'..." -ForegroundColor Yellow
    az group create --name $ResourceGroup --location $Location --output none
    Write-Host "✅ Resource group created`n" -ForegroundColor Green
}

# ============================================================================
# STEP 3: CREATE CONTAINER APPS ENVIRONMENT
# ============================================================================
Write-Host "🏗️  Step 3: Ensuring Container Apps Environment exists..." -ForegroundColor Cyan
$envExists = az containerapp env list `
    --resource-group $ResourceGroup `
    --query "[?name=='$EnvironmentName'] | length(@)" `
    --output tsv 2>$null

if ($envExists -gt 0) {
    Write-Host "✅ Environment '$EnvironmentName' already exists`n" -ForegroundColor Green
} else {
    Write-Host "Creating Container Apps Environment (this may take a few minutes)..." -ForegroundColor Yellow
    az containerapp env create `
        --name $EnvironmentName `
        --resource-group $ResourceGroup `
        --location $Location `
        --output none
    Write-Host "✅ Environment created`n" -ForegroundColor Green
}

# ============================================================================
# DEPLOYMENT HELPER FUNCTION
# ============================================================================
function Deploy-ContainerApp {
    param(
        [string]$ServiceName,
        [int]$Port,
        [string]$IngressType,
        [hashtable]$EnvVars = @{}
    )
    
    $imageName = "$Registry/$RepoLower/$ServiceName`:$ImageTag"
    
    Write-Host "🐳 Deploying $ServiceName..." -ForegroundColor Cyan
    Write-Host "   Image: $imageName" -ForegroundColor Gray
    Write-Host "   Port: $Port | Ingress: $IngressType" -ForegroundColor Gray
    
    # Build environment variables string
    $envVarString = ""
    if ($EnvVars.Count -gt 0) {
        $envVarPairs = $EnvVars.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }
        $envVarString = $envVarPairs -join " "
    }
    
    # Try to create (new deployment)
    Write-Host "   Attempting to create new container app..." -ForegroundColor Gray
    $createCmd = "az containerapp create " +
        "--name $ServiceName " +
        "--resource-group $ResourceGroup " +
        "--environment $EnvironmentName " +
        "--image $imageName " +
        "--target-port $Port " +
        "--ingress $IngressType " +
        "--registry-server $Registry " +
        "--cpu 0.5 --memory 1.0Gi " +
        "--min-replicas 1 --max-replicas 3 " +
        "--output none"
    
    if ($envVarString) {
        $createCmd += " --env-vars $envVarString"
    }
    
    $created = $false
    try {
        Invoke-Expression $createCmd 2>$null
        $created = $true
        Write-Host "✅ $ServiceName deployed successfully" -ForegroundColor Green
    } catch {
        Write-Host "   Container app already exists, updating..." -ForegroundColor Gray
    }
    
    # If create failed, try update (existing deployment)
    if (-not $created) {
        $updateCmd = "az containerapp update " +
            "--name $ServiceName " +
            "--resource-group $ResourceGroup " +
            "--image $imageName " +
            "--output none"
        
        if ($envVarString) {
            $updateCmd += " --set-env-vars $envVarString"
        }
        
        try {
            Invoke-Expression $updateCmd
            Write-Host "✅ $ServiceName updated successfully" -ForegroundColor Green
        } catch {
            Write-Host "⚠️  Failed to deploy $ServiceName - may need manual intervention" -ForegroundColor Yellow
            Write-Host "   Error: $_" -ForegroundColor Red
        }
    }
    
    Write-Host ""
}

# ============================================================================
# STEP 4: DEPLOY SERVICES
# ============================================================================
Write-Host "🚀 Step 4: Deploying Microservices..." -ForegroundColor Cyan
Write-Host ""

# Deploy Auth Service (internal)
Deploy-ContainerApp `
    -ServiceName "auth-service" `
    -Port 8081 `
    -IngressType "internal" `
    -EnvVars @{ "JWT_SECRET" = $JwtSecret }

# Deploy Catalog Service (internal)
Deploy-ContainerApp `
    -ServiceName "catalog-service" `
    -Port 8082 `
    -IngressType "internal" `
    -EnvVars @{ "JWT_SECRET" = $JwtSecret }

# Deploy Order Service (internal)
Deploy-ContainerApp `
    -ServiceName "order-service" `
    -Port 8083 `
    -IngressType "internal" `
    -EnvVars @{ "JWT_SECRET" = $JwtSecret }

# Deploy Payment Service (internal)
Deploy-ContainerApp `
    -ServiceName "payment-service" `
    -Port 8084 `
    -IngressType "internal" `
    -EnvVars @{ "JWT_SECRET" = $JwtSecret }

# Deploy API Gateway (external)
Deploy-ContainerApp `
    -ServiceName "api-gateway" `
    -Port 8080 `
    -IngressType "external" `
    -EnvVars @{
        "JWT_SECRET" = $JwtSecret
        "AUTH_SERVICE_URL" = "http://auth-service"
        "CATALOG_SERVICE_URL" = "http://catalog-service"
        "ORDER_SERVICE_URL" = "http://order-service"
        "PAYMENT_SERVICE_URL" = "http://payment-service"
    }

# ============================================================================
# STEP 5: GET API GATEWAY URL
# ============================================================================
Write-Host "🌐 Step 5: Retrieving API Gateway URL..." -ForegroundColor Cyan
try {
    $gatewayUrl = az containerapp show `
        --name api-gateway `
        --resource-group $ResourceGroup `
        --query properties.configuration.ingress.fqdn `
        --output tsv 2>$null
    
    if ($gatewayUrl) {
        Write-Host "✅ API Gateway URL: https://$gatewayUrl" -ForegroundColor Green
        Write-Host ""
    } else {
        Write-Host "⚠️  Could not retrieve gateway URL (may still be provisioning)" -ForegroundColor Yellow
        Write-Host ""
    }
} catch {
    Write-Host "⚠️  Could not retrieve gateway URL" -ForegroundColor Yellow
    Write-Host ""
}

# ============================================================================
# SUCCESS BANNER
# ============================================================================
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                  🎉 DEPLOYMENT COMPLETE! 🎉                    ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""

if ($gatewayUrl) {
    Write-Host "🌍 Your API is live at: https://$gatewayUrl" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Test your deployment:" -ForegroundColor Yellow
    Write-Host "  • Health Check: https://$gatewayUrl/health" -ForegroundColor White
    Write-Host "  • Swagger UI: https://$gatewayUrl/swagger-ui.html" -ForegroundColor White
    Write-Host "  • Auth Docs: https://$gatewayUrl/auth/v3/api-docs" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Quick Test Commands:" -ForegroundColor Yellow
    Write-Host "  curl https://$gatewayUrl/health" -ForegroundColor Gray
    Write-Host '  curl -X POST https://' + $gatewayUrl + '/auth/register -H "Content-Type: application/json" -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\"}"' -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Deployment Summary:" -ForegroundColor Yellow
Write-Host "  ✅ Resource Group: $ResourceGroup" -ForegroundColor Green
Write-Host "  ✅ Environment: $EnvironmentName" -ForegroundColor Green
Write-Host "  ✅ Auth Service (internal)" -ForegroundColor Green
Write-Host "  ✅ Catalog Service (internal)" -ForegroundColor Green
Write-Host "  ✅ Order Service (internal)" -ForegroundColor Green
Write-Host "  ✅ Payment Service (internal)" -ForegroundColor Green
Write-Host "  ✅ API Gateway (external)" -ForegroundColor Green
Write-Host ""

Write-Host "📝 Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Test the health endpoint" -ForegroundColor White
Write-Host "  2. Try the Swagger UI" -ForegroundColor White
Write-Host "  3. Register a test user via /auth/register" -ForegroundColor White
Write-Host ""

Write-Host "🔍 Monitor your deployment:" -ForegroundColor Yellow
Write-Host "  az containerapp list --resource-group $ResourceGroup --output table" -ForegroundColor Gray
Write-Host ""

Write-Host "🗑️  To delete everything:" -ForegroundColor Yellow
Write-Host "  az group delete --name $ResourceGroup --yes --no-wait" -ForegroundColor Gray
Write-Host ""
