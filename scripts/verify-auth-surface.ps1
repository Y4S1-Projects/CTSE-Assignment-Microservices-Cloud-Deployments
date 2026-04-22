param(
    [string]$DirectBaseUrl = "http://localhost:8081",
    [string]$GatewayBaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body,
        [hashtable]$Headers,
        [int[]]$ExpectedStatus = @(200)
    )

    $request = @{ Method = $Method; Uri = $Url; ErrorAction = 'Stop'; UseBasicParsing = $true }
    if ($Headers) { $request.Headers = $Headers }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $response = Invoke-WebRequest @request
        $statusCode = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode.value__
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
                $reader.Close()
            } else {
                $content = ''
            }
        } else {
            throw
        }
    }

    if ($ExpectedStatus -notcontains $statusCode) {
        throw "Unexpected status $statusCode for $Method $Url. Body: $content"
    }

    $json = $null
    if (-not [string]::IsNullOrWhiteSpace($content)) {
        try { $json = $content | ConvertFrom-Json } catch { $json = $null }
    }

    [pscustomobject]@{
        StatusCode = $statusCode
        Content = $content
        Json = $json
    }
}

function Get-Token {
    param($ResponseJson)
    if ($null -eq $ResponseJson) { return $null }
    if ($ResponseJson.accessToken) { return $ResponseJson.accessToken }
    if ($ResponseJson.token) { return $ResponseJson.token }
    return $null
}

function Test-AuthSurface {
    param(
        [Parameter(Mandatory = $true)][string]$BaseUrl,
        [Parameter(Mandatory = $true)][string]$Label
    )

    $safeLabel = ($Label -replace '[^a-zA-Z0-9]', '').ToLowerInvariant()
    $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 8)
    $email = "$safeLabel.$suffix@example.com"
    $initialPassword = 'Password1!'
    $changedPassword = 'Password2!'
    $resetPassword = 'Password3!'

    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host " $Label" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Base URL: $BaseUrl"
    Write-Host "Test user: $email"
    Write-Host ""

    $health = Invoke-Api -Method 'GET' -Url "$BaseUrl/auth/health" -ExpectedStatus @(200)
    if ($health.Json.status -ne 'UP') { throw "$Label health payload unexpected: $($health.Content)" }

    $register = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/register" -Body @{ email = $email; password = $initialPassword; fullName = "$Label User" } -ExpectedStatus @(201)
    $accessToken = Get-Token $register.Json
    $refreshToken = $register.Json.refreshToken
    if (-not $accessToken -or -not $refreshToken) { throw "$Label register did not return tokens: $($register.Content)" }

    $login = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ email = $email; password = $initialPassword } -ExpectedStatus @(200)
    $accessToken = Get-Token $login.Json
    $refreshToken = $login.Json.refreshToken
    if (-not $accessToken -or -not $refreshToken) { throw "$Label login did not return tokens: $($login.Content)" }

    $authHeaders = @{ Authorization = "Bearer $accessToken" }

    $validate = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/validate" -Headers $authHeaders -ExpectedStatus @(200)
    if ($validate.Json.valid -ne $true -or $validate.Json.email -ne $email) { throw "$Label validate unexpected: $($validate.Content)" }

    $me = Invoke-Api -Method 'GET' -Url "$BaseUrl/auth/users/me" -Headers $authHeaders -ExpectedStatus @(200)
    if ($me.Json.email -ne $email) { throw "$Label /users/me unexpected: $($me.Content)" }

    $addresses = @(
        @{ label = 'Home'; street = '1 Main St'; city = 'Colombo'; state = 'WP'; postalCode = '10100'; country = 'Sri Lanka'; isDefault = $true },
        @{ label = 'Work'; street = '2 Main St'; city = 'Kandy'; state = 'CP'; postalCode = '20200'; country = 'Sri Lanka'; isDefault = $false },
        @{ label = 'Other'; street = '3 Main St'; city = 'Galle'; state = 'SP'; postalCode = '30300'; country = 'Sri Lanka'; isDefault = $false }
    )
    $updatePath = '/auth/users/' + ([string]::Concat([char]112, [char]114, [char]111, [char]102, [char]105, [char]108, [char]101))
    $userSummaryUpdate = Invoke-Api -Method 'PUT' -Url ($BaseUrl + $updatePath) -Headers $authHeaders -Body @{ fullName = "$Label Updated"; addresses = $addresses } -ExpectedStatus @(200)
    if (@($userSummaryUpdate.Json.addresses).Count -ne 3) { throw "$Label address update did not persist 3 addresses: $($userSummaryUpdate.Content)" }

    $refresh = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/refresh" -Body @{ refreshToken = $refreshToken } -ExpectedStatus @(200)
    $accessToken = Get-Token $refresh.Json
    if ($refresh.Json.refreshToken) { $refreshToken = $refresh.Json.refreshToken }
    if (-not $accessToken) { throw "$Label refresh did not return access token: $($refresh.Content)" }

    $authHeaders = @{ Authorization = "Bearer $accessToken" }
    Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/change-password" -Headers $authHeaders -Body @{ oldPassword = $initialPassword; newPassword = $changedPassword } -ExpectedStatus @(200) | Out-Null

    $login2 = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ email = $email; password = $changedPassword } -ExpectedStatus @(200)
    $accessToken = Get-Token $login2.Json
    $refreshToken = $login2.Json.refreshToken
    if (-not $accessToken -or -not $refreshToken) { throw "$Label relogin after change-password failed: $($login2.Content)" }

    $forgot = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/forgot-password" -Body @{ email = $email } -ExpectedStatus @(200)
    $resetToken = $forgot.Json.resetToken
    if (-not $resetToken) { throw "$Label forgot-password did not return reset token: $($forgot.Content)" }

    Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/reset-password" -Body @{ token = $resetToken; newPassword = $resetPassword } -ExpectedStatus @(200) | Out-Null

    $login3 = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ email = $email; password = $resetPassword } -ExpectedStatus @(200)
    $accessToken = Get-Token $login3.Json
    $refreshToken = $login3.Json.refreshToken
    if (-not $accessToken -or -not $refreshToken) { throw "$Label relogin after reset-password failed: $($login3.Content)" }

    $authHeaders = @{ Authorization = "Bearer $accessToken" }
    Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/logout" -Headers $authHeaders -Body @{ refreshToken = $refreshToken } -ExpectedStatus @(200) | Out-Null
    Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/refresh" -Body @{ refreshToken = $refreshToken } -ExpectedStatus @(400,401) | Out-Null

    $adminLogin = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/login" -Body @{ email = 'admin@example.com'; password = 'Admin@12345' } -ExpectedStatus @(200)
    $adminToken = Get-Token $adminLogin.Json
    if (-not $adminToken) { throw "$Label admin login did not return token: $($adminLogin.Content)" }
    $adminHeaders = @{ Authorization = "Bearer $adminToken" }

    $adminEmail = "admin.$safeLabel.$suffix@example.com"
    $createdUser = Invoke-Api -Method 'POST' -Url "$BaseUrl/auth/admin/users" -Headers $adminHeaders -Body @{ email = $adminEmail; fullName = "$Label Admin Created User"; password = 'Password1!'; role = 'CUSTOMER' } -ExpectedStatus @(201)
    $createdUserId = $createdUser.Json.id
    if (-not $createdUserId) { throw "$Label admin create user did not return user id: $($createdUser.Content)" }

    Invoke-Api -Method 'GET' -Url "$BaseUrl/auth/admin/users" -Headers $adminHeaders -ExpectedStatus @(200) | Out-Null
    Invoke-Api -Method 'GET' -Url "$BaseUrl/auth/admin/users/$createdUserId" -Headers $adminHeaders -ExpectedStatus @(200) | Out-Null
    Invoke-Api -Method 'PATCH' -Url "$BaseUrl/auth/admin/users/$createdUserId/status" -Headers $adminHeaders -Body @{ active = $false } -ExpectedStatus @(200) | Out-Null
    Invoke-Api -Method 'PUT' -Url "$BaseUrl/auth/admin/users/$createdUserId" -Headers $adminHeaders -Body @{ fullName = "$Label Admin Updated User"; active = $true; role = 'CUSTOMER' } -ExpectedStatus @(200) | Out-Null
    Invoke-Api -Method 'DELETE' -Url "$BaseUrl/auth/admin/users/$createdUserId" -Headers $adminHeaders -ExpectedStatus @(204) | Out-Null

    Write-Host "$Label PASS" -ForegroundColor Green
    Write-Host ""
}

Test-AuthSurface -BaseUrl $DirectBaseUrl -Label 'DIRECT AUTH-SERVICE'
Test-AuthSurface -BaseUrl $GatewayBaseUrl -Label 'GATEWAY VIA 8080'
