$ErrorActionPreference='Stop'
function Invoke-Api {
  param([string]$Method,[string]$Url,[object]$Body=$null,[hashtable]$Headers=@{})
  try {
    $params = @{ Method=$Method; Uri=$Url; Headers=$Headers }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 8); $params.ContentType='application/json' }
    $resp = Invoke-WebRequest @params
    $json = $null
    try { $json = $resp.Content | ConvertFrom-Json } catch { $json = $resp.Content }
    return [pscustomobject]@{ Status=[int]$resp.StatusCode; Body=$json }
  } catch {
    $status = 0; $body = $null
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode.value__
      $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
      $content = $reader.ReadToEnd()
      try { $body = $content | ConvertFrom-Json } catch { $body = $content }
    } else { $body = $_.Exception.Message }
    return [pscustomobject]@{ Status=$status; Body=$body }
  }
}

function Run-Matrix {
  param([string]$Base,[string]$Label)
  $uid = [guid]::NewGuid().ToString('N').Substring(0,8)
  $email = "$uid@test.com"; $username = "u$uid"
  $baseAuth = "$Base/auth"
  $results = @()

  $rHealth = Invoke-Api GET "$baseAuth/health"
  $results += [pscustomobject]@{Case='health';Ok=($rHealth.Status -eq 200);Status=$rHealth.Status}

  $rReg = Invoke-Api POST "$baseAuth/register" @{username=$username;email=$email;password='Password1!';fullName='Test User'}
  $results += [pscustomobject]@{Case='register_ok';Ok=($rReg.Status -eq 201);Status=$rReg.Status}
  $access = $rReg.Body.accessToken; $refresh = $rReg.Body.refreshToken

  $rDup = Invoke-Api POST "$baseAuth/register" @{username=$username;email=$email;password='Password1!';fullName='Dup'}
  $results += [pscustomobject]@{Case='register_duplicate';Ok=($rDup.Status -eq 409);Status=$rDup.Status}

  $rLoginOk = Invoke-Api POST "$baseAuth/login" @{email=$email;password='Password1!'}
  $results += [pscustomobject]@{Case='login_ok';Ok=($rLoginOk.Status -eq 200);Status=$rLoginOk.Status}

  $rLoginBad = Invoke-Api POST "$baseAuth/login" @{email=$email;password='Wrong!'}
  $results += [pscustomobject]@{Case='login_bad_password';Ok=($rLoginBad.Status -eq 401);Status=$rLoginBad.Status}

  $rValidateOk = Invoke-Api POST "$baseAuth/validate" $null @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='validate_ok';Ok=($rValidateOk.Status -eq 200);Status=$rValidateOk.Status}

  $rValidateMissing = Invoke-Api POST "$baseAuth/validate"
  $results += [pscustomobject]@{Case='validate_missing_header';Ok=($rValidateMissing.Status -eq 401);Status=$rValidateMissing.Status}

  $rRefreshOk = Invoke-Api POST "$baseAuth/refresh" @{refreshToken=$refresh}
  $results += [pscustomobject]@{Case='refresh_ok';Ok=($rRefreshOk.Status -eq 200);Status=$rRefreshOk.Status}
  $refresh2 = $rRefreshOk.Body.refreshToken

  $rMe = Invoke-Api GET "$baseAuth/users/me" $null @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='users_me_ok';Ok=($rMe.Status -eq 200);Status=$rMe.Status}

  $rProfile = Invoke-Api PUT "$baseAuth/users/profile" @{fullName='Updated Name'} @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='users_profile_update';Ok=($rProfile.Status -eq 200);Status=$rProfile.Status}

  $rAddrCreate = Invoke-Api POST "$baseAuth/users/addresses" @{street='1 Main';city='Colombo';postalCode='10000';isDefault=$true} @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='address_create';Ok=($rAddrCreate.Status -eq 201);Status=$rAddrCreate.Status}
  $addrId = $rAddrCreate.Body.id

  $rAddrList = Invoke-Api GET "$baseAuth/users/addresses" $null @{Authorization="Bearer $access"}
  $listOk = ($rAddrList.Status -eq 200 -and $rAddrList.Body.Count -ge 1)
  $results += [pscustomobject]@{Case='address_list';Ok=$listOk;Status=$rAddrList.Status}

  if ($addrId) {
    $rAddrUpdate = Invoke-Api PUT "$baseAuth/users/addresses/$addrId" @{street='2 Main';city='Kandy';postalCode='20000';isDefault=$false} @{Authorization="Bearer $access"}
    $results += [pscustomobject]@{Case='address_update';Ok=($rAddrUpdate.Status -eq 200);Status=$rAddrUpdate.Status}

    $rAddrDel = Invoke-Api DELETE "$baseAuth/users/addresses/$addrId" $null @{Authorization="Bearer $access"}
    $results += [pscustomobject]@{Case='address_delete';Ok=($rAddrDel.Status -eq 204);Status=$rAddrDel.Status}
  }

  $rChangeOk = Invoke-Api POST "$baseAuth/change-password" @{oldPassword='Password1!';newPassword='NewPassword2!'} @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='change_password_ok';Ok=($rChangeOk.Status -eq 200);Status=$rChangeOk.Status}

  $rForgot = Invoke-Api POST "$baseAuth/forgot-password" @{email=$email}
  $results += [pscustomobject]@{Case='forgot_password_ok';Ok=($rForgot.Status -eq 200);Status=$rForgot.Status}
  $resetToken = $rForgot.Body.resetToken

  if ($resetToken) {
    $rReset = Invoke-Api POST "$baseAuth/reset-password" @{token=$resetToken;newPassword='ResetPassword3!'}
    $results += [pscustomobject]@{Case='reset_password_ok';Ok=($rReset.Status -eq 200);Status=$rReset.Status}
  }

  $rAdminLogin = Invoke-Api POST "$baseAuth/login" @{email='admin@local.test';password='Admin@12345'}
  $results += [pscustomobject]@{Case='admin_login_ok';Ok=($rAdminLogin.Status -eq 200);Status=$rAdminLogin.Status}
  $adminAccess = $rAdminLogin.Body.accessToken

  $rAdminUsers = Invoke-Api GET "$baseAuth/admin/users" $null @{Authorization="Bearer $adminAccess"}
  $results += [pscustomobject]@{Case='admin_users_as_admin';Ok=($rAdminUsers.Status -eq 200);Status=$rAdminUsers.Status}

  $rAdminBlocked = Invoke-Api GET "$baseAuth/admin/users" $null @{Authorization="Bearer $access"}
  $results += [pscustomobject]@{Case='admin_users_as_customer_forbidden';Ok=($rAdminBlocked.Status -eq 403);Status=$rAdminBlocked.Status}

  $rLogout = Invoke-Api POST "$baseAuth/logout" @{refreshToken=$refresh2}
  $results += [pscustomobject]@{Case='logout_ok';Ok=($rLogout.Status -eq 200);Status=$rLogout.Status}

  $rRefreshRevoked = Invoke-Api POST "$baseAuth/refresh" @{refreshToken=$refresh2}
  $results += [pscustomobject]@{Case='refresh_revoked_fails';Ok=($rRefreshRevoked.Status -eq 401);Status=$rRefreshRevoked.Status}

  $passed = ($results | Where-Object { -not $_.Ok }).Count -eq 0
  Write-Host "==== $Label ===="
  $results | ForEach-Object { Write-Host ("{0,-36} status={1} ok={2}" -f $_.Case,$_.Status,$_.Ok) }
  Write-Host "SUMMARY $Label PASS=$passed TOTAL=$($results.Count)"
  return $passed
}

$directPass = Run-Matrix -Base 'http://localhost:8081' -Label 'DIRECT_AUTH_8081'
$gwPass = Run-Matrix -Base 'http://localhost:8080' -Label 'GATEWAY_8080'

if ($directPass -and $gwPass) { Write-Host 'OVERALL_PASS=True' } else { Write-Host 'OVERALL_PASS=False'; exit 1 }
