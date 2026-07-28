# test-everything.ps1
#
# Full backend smoke test: health checks, signup/login, admin service-type
# CRUD, booking a reservation, both rate limiters, and circuit breaker
# baseline. Run this before doing manual frontend testing - if this passes,
# you know the API layer is solid and any issue you find in the browser is
# a frontend bug, not a backend one.
#
# USAGE:
#   powershell -ExecutionPolicy Bypass -File .\test-everything.ps1
#   .\test-everything.ps1 -AdminPassword "YourChangedAdminPassword"

param(
    [string]$CoreBaseUrl = "http://localhost:8080/api",
    [string]$ChatBaseUrl = "http://localhost:8081/api",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "ChangeMe123"
)

$ErrorActionPreference = "Stop"
$results = @()

function Test-Step {
    param([string]$Name, [scriptblock]$Action)
    Write-Host "`n>> $Name" -ForegroundColor Cyan
    try {
        & $Action
        Write-Host "   PASS" -ForegroundColor Green
        $script:results += [pscustomobject]@{ Step = $Name; Result = "PASS"; Detail = "" }
    } catch {
        Write-Host "   FAIL: $($_.Exception.Message)" -ForegroundColor Red
        $script:results += [pscustomobject]@{ Step = $Name; Result = "FAIL"; Detail = $_.Exception.Message }
    }
}

function Invoke-Json {
    param([string]$Uri, [string]$Method = "GET", $Body = $null, [hashtable]$Headers = @{})
    $params = @{ Uri = $Uri; Method = $Method; Headers = $Headers; ContentType = "application/json" }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Depth 6) }
    return Invoke-RestMethod @params
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$testUsername = "smoketest_$suffix"
$testEmail = "smoketest_$suffix@example.com"
$testPassword = "TestPass123"
$testPhone = "9876543210"

$customerToken = $null
$adminToken = $null
$serviceTypeId = $null
$newServiceTypeId = $null
$reservationId = $null

# ---------------------------------------------------------------------------
Test-Step "Core-service health check" {
    $health = Invoke-RestMethod -Uri "$CoreBaseUrl/../actuator/health"
    if ($health.status -ne "UP") { throw "status was $($health.status)" }
}

Test-Step "AI-chat-service health check" {
    $health = Invoke-RestMethod -Uri "$ChatBaseUrl/../actuator/health"
    if ($health.status -ne "UP") { throw "status was $($health.status)" }
}

# ---------------------------------------------------------------------------
Test-Step "Signup a new customer" {
    $body = @{ username = $testUsername; email = $testEmail; password = $testPassword; phone = $testPhone }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/signup" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken in signup response" }
    $script:customerToken = $response.accessToken
    if ($response.role -ne "CUSTOMER") { throw "expected role CUSTOMER, got $($response.role)" }
}

Test-Step "Login as bootstrap admin" {
    $body = @{ username = $AdminUsername; password = $AdminPassword }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken in login response" }
    $script:adminToken = $response.accessToken
    if ($response.role -ne "ADMIN") { throw "expected role ADMIN, got $($response.role)" }
}

# ---------------------------------------------------------------------------
Test-Step "Public: list active service types" {
    $types = Invoke-RestMethod -Uri "$CoreBaseUrl/service-types"
    if ($types.Count -eq 0) { throw "no service types returned" }
    $script:serviceTypeId = $types[0].serviceTypeId
    Write-Host "   ($($types.Count) active service types, using id=$($script:serviceTypeId) for booking test)"
}

Test-Step "Admin: create a new service type" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ name = "SmokeTest_$suffix"; description = "Created by automated test"; basePrice = 199 }
    $created = Invoke-Json -Uri "$CoreBaseUrl/admin/service-types" -Method POST -Body $body -Headers $headers
    if (-not $created.serviceTypeId) { throw "no serviceTypeId returned" }
    $script:newServiceTypeId = $created.serviceTypeId
}

Test-Step "Admin: new service type appears in admin list-all" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $all = Invoke-RestMethod -Uri "$CoreBaseUrl/admin/service-types" -Headers $headers
    $match = $all | Where-Object { $_.serviceTypeId -eq $newServiceTypeId }
    if (-not $match) { throw "created service type not found in admin list" }
    if (-not $match.active) { throw "expected active=true on creation" }
}

Test-Step "Admin: deactivate the service type" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $updated = Invoke-RestMethod -Uri "$CoreBaseUrl/admin/service-types/$newServiceTypeId/status?active=false" -Method PATCH -Headers $headers
    if ($updated.active) { throw "expected active=false after deactivation" }
}

Test-Step "Deactivated service type is hidden from public listing" {
    $types = Invoke-RestMethod -Uri "$CoreBaseUrl/service-types"
    $match = $types | Where-Object { $_.serviceTypeId -eq $newServiceTypeId }
    if ($match) { throw "deactivated service type still visible publicly" }
}

Test-Step "Admin: reactivate the service type" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $updated = Invoke-RestMethod -Uri "$CoreBaseUrl/admin/service-types/$newServiceTypeId/status?active=true" -Method PATCH -Headers $headers
    if (-not $updated.active) { throw "expected active=true after reactivation" }
}

# ---------------------------------------------------------------------------
Test-Step "Customer: create a reservation" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $body = @{
        serviceTypeId    = $serviceTypeId
        reservationDate  = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
        timeSlot         = "10:00-12:00"
        address          = "123 Smoke Test Street, Bengaluru"
        telephone        = $testPhone
        comments         = "Automated test booking"
    }
    $created = Invoke-Json -Uri "$CoreBaseUrl/reservations" -Method POST -Body $body -Headers $headers
    if (-not $created.reservationId) { throw "no reservationId returned" }
    $script:reservationId = $created.reservationId
    if ($created.status -ne "PENDING") { throw "expected status PENDING, got $($created.status)" }
}

Test-Step "Customer: new reservation appears in my-reservations" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $mine = Invoke-RestMethod -Uri "$CoreBaseUrl/reservations/me" -Headers $headers
    $match = $mine | Where-Object { $_.reservationId -eq $reservationId }
    if (-not $match) { throw "created reservation not found in /reservations/me" }
}

Test-Step "Customer: cancel the reservation (cleanup)" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $body = @{ status = "CANCELLED" }
    $updated = Invoke-Json -Uri "$CoreBaseUrl/reservations/$reservationId/status" -Method PATCH -Body $body -Headers $headers
    if ($updated.status -ne "CANCELLED") { throw "expected status CANCELLED, got $($updated.status)" }
}

# ---------------------------------------------------------------------------
Test-Step "Auth rate limiter trips after 5 failed logins" {
    $tripped = $false
    for ($i = 1; $i -le 6; $i++) {
        try {
            Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body @{ username = $testUsername; password = "wrong-password" }
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 429) { $tripped = $true }
        }
    }
    if (-not $tripped) { throw "never got a 429 after 6 failed login attempts" }
}

Test-Step "Chat rate limiter trips within 20 messages" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $tripped = $false
    for ($i = 1; $i -le 20; $i++) {
        $body = @{ messages = @(@{ role = "user"; content = "smoke test message $i" }) }
        try {
            Invoke-Json -Uri "$ChatBaseUrl/chat/message" -Method POST -Body $body -Headers $headers | Out-Null
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 429) { $tripped = $true; break }
        }
        Start-Sleep -Milliseconds 150
    }
    if (-not $tripped) { throw "never got a 429 after 20 chat messages" }
}

# ---------------------------------------------------------------------------
Test-Step "Circuit breaker actuator endpoint responds" {
    $response = Invoke-WebRequest -Uri "$ChatBaseUrl/../actuator/circuitbreakers" -UseBasicParsing
    $json = [System.Text.Encoding]::UTF8.GetString($response.Content) | ConvertFrom-Json
    if (-not $json.circuitBreakers.groq) { throw "no 'groq' circuit breaker instance found" }
    Write-Host "   groq breaker state: $($json.circuitBreakers.groq.state)"
}

# ---------------------------------------------------------------------------
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
$results | Format-Table -AutoSize

$failCount = ($results | Where-Object { $_.Result -eq "FAIL" }).Count
$passCount = ($results | Where-Object { $_.Result -eq "PASS" }).Count

if ($failCount -eq 0) {
    Write-Host "`nAll $passCount checks passed. Backend is solid - move on to manual frontend testing." -ForegroundColor Green
} else {
    Write-Host "`n$failCount of $($results.Count) checks FAILED. Fix these before testing the frontend - a backend" -ForegroundColor Red
    Write-Host "issue found here will just look like a confusing frontend bug otherwise." -ForegroundColor Red
}
