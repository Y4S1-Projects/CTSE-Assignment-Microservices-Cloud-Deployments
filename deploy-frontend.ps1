# Deploy Frontend to Azure App Service
# Deploys the Next.js frontend from GitHub Container Registry

param(
    [string]$ResourceGroup = "ctse-rg-sea",
    [string]$Location = "southeastasia",
    [string]$AppServicePlanName = "ctse-plan",
    [string]$WebAppName = "ctse-frontend-$(Get-Random -Minimum 1000 -Maximum 9999)",
    [string]$GitHubRepo = "y4s1-projects/ctse-assignment-microservices-cloud-deployments",
    [string]$ImageTag = "latest",
    [string]$GatewayUrl = ""
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

function Deploy-Frontend {
    Write-Section "Step 2: Deploy Frontend Web App"
    
    $registry = "ghcr.io"
    $repoLower = $GitHubRepo.ToLower()
    $image = "$registry/$repoLower/frontend`:$ImageTag"
    
    Write-Host ""
    Write-Host "Creating Web App: $WebAppName" -ForegroundColor Cyan
    Write-Host "Image: $image" -ForegroundColor Gray
    Write-Host "Plan: $AppServicePlanName" -ForegroundColor Gray
    
    try {
        # Create web app
        az webapp create `
            --resource-group $ResourceGroup `
            --plan $AppServicePlanName `
            --name $WebAppName `
            --deployment-container-image-name $image `
            --output none
        
        Write-Host "Created Web App: $WebAppName" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Web app may already exist or error occurred" -ForegroundColor Yellow
    }
    
    try {
        # Configure container settings
        az webapp config container set `
            --name $WebAppName `
            --resource-group $ResourceGroup `
            --docker-custom-image-name $image `
            --docker-registry-server-url "https://$registry" `
            --output none
        
        Write-Host "Configured container settings" -ForegroundColor Green
    } catch {
        Write-Host "⚠️  Error configuring container" -ForegroundColor Yellow
    }
    
    # Set environment variables
    if (-not [string]::IsNullOrWhiteSpace($GatewayUrl)) {
        az webapp config appsettings set `
            --resource-group $ResourceGroup `
            --name $WebAppName `
            --settings NEXT_PUBLIC_API_URL=$GatewayUrl `
            --output none
        
        Write-Host "Set API Gateway URL: $GatewayUrl" -ForegroundColor Green
    }
}

function Show-Result {
    Write-Section "Step 3: Deployment Complete"
    
    try {
        $webAppUrl = az webapp show --name $WebAppName --resource-group $ResourceGroup --query "defaultHostName" --output tsv 2>$null
    } catch {
        $webAppUrl = ""
    }
    
    Write-Host ""
    Write-Host "Frontend Web App: $WebAppName" -ForegroundColor Cyan
    
    if (-not [string]::IsNullOrWhiteSpace($webAppUrl)) {
        Write-Host "URL: https://$webAppUrl" -ForegroundColor Green
        Write-Host ""
        Write-Host "Access your frontend at: https://$webAppUrl" -ForegroundColor Yellow
    } else {
        Write-Host "Could not retrieve URL. Check Azure portal:" -ForegroundColor Yellow
        Write-Host "az webapp show --name $WebAppName --resource-group $ResourceGroup --query defaultHostName" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "Monitor logs:" -ForegroundColor Yellow
    Write-Host "az webapp log tail --name $WebAppName --resource-group $ResourceGroup" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Delete web app:" -ForegroundColor Yellow
    Write-Host "az webapp delete --name $WebAppName --resource-group $ResourceGroup" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Frontend Deployment Configuration:" -ForegroundColor Yellow
Write-Host "Resource Group: $ResourceGroup" -ForegroundColor White
Write-Host "Location: $Location" -ForegroundColor White
Write-Host "App Service Plan: $AppServicePlanName" -ForegroundColor White
Write-Host "Image Tag: $ImageTag" -ForegroundColor White

if (-not [string]::IsNullOrWhiteSpace($GatewayUrl)) {
    Write-Host "Gateway URL: $GatewayUrl" -ForegroundColor White
}

Ensure-AzureLogin
Deploy-Frontend
Show-Result
