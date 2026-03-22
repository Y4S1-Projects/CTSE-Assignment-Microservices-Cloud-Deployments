param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = 'Stop'

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Auth via API Gateway E2E Verification" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host ""

$email = "gateway.e2e.$([Guid]::NewGuid().ToString('N').Substring(0, 8))@example.com"
$password = "Password1!"

Write-Host "[1/7] Register via gateway" -ForegroundColor Yellow
$registerBody = @{
    email = $email
    password = $password
    fullName = "Gateway E2E User"
} | ConvertTo-Json

$registerResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType 'application/json' -Body $registerBody
$accessToken = if ($registerResponse.accessToken) { $registerResponse.accessToken } else { $registerResponse.token }
$refreshToken = $registerResponse.refreshToken

if (-not $accessToken -or -not $refreshToken) {
    throw "Registration did not return expected tokens"
}

$headers = @{ Authorization = "Bearer $accessToken" }

Write-Host "[2/7] Read profile via /auth/users/me" -ForegroundColor Yellow
$me = Invoke-RestMethod -Uri "$BaseUrl/auth/users/me" -Method Get -Headers $headers
if ($me.email -ne $email) {
    throw "Profile email mismatch"
}

Write-Host "[3/7] Update profile with exactly 3 addresses" -ForegroundColor Yellow
$a1 = @{ street = 'No 1'; city = 'Colombo'; postalCode = '10100'; isDefault = $true }
$a2 = @{ street = 'No 2'; city = 'Kandy'; postalCode = '20200'; isDefault = $false }
$a3 = @{ street = 'No 3'; city = 'Galle'; postalCode = '30300'; isDefault = $false }
$profileBody = @{ phone = '0712345678'; addresses = @($a1, $a2, $a3) } | ConvertTo-Json -Depth 6
$profile = Invoke-RestMethod -Uri "$BaseUrl/auth/users/profile" -Method Put -Headers $headers -ContentType 'application/json' -Body $profileBody

if (($profile.addresses | Measure-Object).Count -ne 3) {
    throw "Profile did not persist 3 addresses"
}

Write-Host "[4/7] Ensure >3 addresses is rejected" -ForegroundColor Yellow
$a4 = @{ street = 'No 4'; city = 'Jaffna'; postalCode = '40400'; isDefault = $false }
$invalidProfileBody = @{ addresses = @($a1, $a2, $a3, $a4) } | ConvertTo-Json -Depth 6
$overLimitRejected = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/auth/users/profile" -Method Put -Headers $headers -ContentType 'application/json' -Body $invalidProfileBody | Out-Null
} catch {
    if ($_.Exception.Message -match '\(400\)') {
        $overLimitRejected = $true
    } else {
        throw
    }
}
if (-not $overLimitRejected) {
    throw "Expected 400 when sending more than 3 addresses"
}

Write-Host "[5/7] Refresh access token" -ForegroundColor Yellow
$refreshBody = @{ refreshToken = $refreshToken } | ConvertTo-Json
$refreshResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/refresh" -Method Post -ContentType 'application/json' -Body $refreshBody
$newToken = if ($refreshResponse.accessToken) { $refreshResponse.accessToken } else { $refreshResponse.token }
if (-not $newToken) {
    throw "Refresh did not return a new access token"
}

Write-Host "[6/7] Logout with refreshed token" -ForegroundColor Yellow
$logoutHeaders = @{ Authorization = "Bearer $newToken" }
Invoke-RestMethod -Uri "$BaseUrl/auth/logout" -Method Post -Headers $logoutHeaders -ContentType 'application/json' -Body $refreshBody | Out-Null

Write-Host "[7/7] Ensure refresh token is revoked after logout" -ForegroundColor Yellow
$refreshBlocked = $false
try {
    Invoke-RestMethod -Uri "$BaseUrl/auth/refresh" -Method Post -ContentType 'application/json' -Body $refreshBody | Out-Null
} catch {
    if ($_.Exception.Message -match '\(401\)') {
        $refreshBlocked = $true
    } else {
        throw
    }
}

if (-not $refreshBlocked) {
    throw "Expected refresh token to be invalid after logout"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Gateway E2E Verification PASSED" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ("Test user: {0}" -f $email)
Write-Host ""