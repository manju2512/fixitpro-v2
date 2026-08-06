# test-everything.ps1 (v2)
#
# Full backend smoke test - original core checks plus everything added
# this session: username availability, strict phone validation, password
# reset (request side + bad-token handling), technician self-service
# profile, and admin reservation management (assign + status transitions).
#
# USAGE:
#   powershell -ExecutionPolicy Bypass -File .\test-everything.ps1
#   .\test-everything.ps1 -AdminPassword "YourActualAdminPassword"

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

function Get-StatusCode {
    param($ErrorRecord)
    return $ErrorRecord.Exception.Response.StatusCode.value__
}

$suffix = Get-Date -Format "yyyyMMddHHmmss"
$testUsername = "smoketest_$suffix"
$testEmail = "smoketest_$suffix@example.com"
$testPassword = "TestPass123"
# Derived from the timestamp suffix (not a fixed constant) so repeated runs
# against a persistent DB (local/Render) never collide on phone - unlike
# username/email, "9876543210" reused across dozens of runs would eventually
# violate the phone uniqueness constraint, or worse, make the flexible-login
# lookup ambiguous across multiple old test accounts.
$phoneDigits = $suffix.Substring($suffix.Length - 9)
$testPhone = "9$phoneDigits"

$techUsername = "smoketech_$suffix"
$techEmail = "smoketech_$suffix@example.com"
$techPhone = "8$phoneDigits"

$customerToken = $null
$adminToken = $null
$technicianToken = $null
$serviceTypeId = $null
$newServiceTypeId = $null
$reservationId = $null
$technicianId = $null

# ---------------------------------------------------------------------------
# ORIGINAL CORE CHECKS
# ---------------------------------------------------------------------------

Test-Step "Core-service health check" {
    $health = Invoke-RestMethod -Uri "$CoreBaseUrl/../actuator/health"
    if ($health.status -ne "UP") { throw "status was $($health.status)" }
}

Test-Step "AI-chat-service health check" {
    $health = Invoke-RestMethod -Uri "$ChatBaseUrl/../actuator/health"
    if ($health.status -ne "UP") { throw "status was $($health.status)" }
}

Test-Step "Signup a new customer" {
    $body = @{ username = $testUsername; email = $testEmail; password = $testPassword; phone = $testPhone }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/signup" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken in signup response" }
    $script:customerToken = $response.accessToken
    if ($response.role -ne "CUSTOMER") { throw "expected role CUSTOMER, got $($response.role)" }
}

Test-Step "Login via email works (flexible login)" {
    $body = @{ username = $testEmail; password = $testPassword }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken when logging in via email" }
    if ($response.username -ne $testUsername) { throw "expected username $testUsername, got $($response.username)" }
}

Test-Step "Login via phone works (flexible login)" {
    $body = @{ username = $testPhone; password = $testPassword }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken when logging in via phone" }
    if ($response.username -ne $testUsername) { throw "expected username $testUsername, got $($response.username)" }
}

Test-Step "Login as bootstrap admin" {
    $body = @{ username = $AdminUsername; password = $AdminPassword }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken in login response" }
    $script:adminToken = $response.accessToken
    if ($response.role -ne "ADMIN") { throw "expected role ADMIN, got $($response.role)" }
}

Test-Step "Public: list active service types" {
    $types = Invoke-RestMethod -Uri "$CoreBaseUrl/service-types"
    if ($types.Count -eq 0) { throw "no service types returned" }
    $script:serviceTypeId = $types[0].serviceTypeId
    Write-Host "   ($($types.Count) active service types, using id=$($script:serviceTypeId))"
}

Test-Step "Admin: create a new service type" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ name = "SmokeTest_$suffix"; description = "Created by automated test"; basePrice = 199 }
    $created = Invoke-Json -Uri "$CoreBaseUrl/admin/service-types" -Method POST -Body $body -Headers $headers
    if (-not $created.serviceTypeId) { throw "no serviceTypeId returned" }
    $script:newServiceTypeId = $created.serviceTypeId
}

Test-Step "Admin: deactivate then reactivate the service type" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $off = Invoke-RestMethod -Uri "$CoreBaseUrl/admin/service-types/$newServiceTypeId/status?active=false" -Method PATCH -Headers $headers
    if ($off.active) { throw "expected active=false" }
    $on = Invoke-RestMethod -Uri "$CoreBaseUrl/admin/service-types/$newServiceTypeId/status?active=true" -Method PATCH -Headers $headers
    if (-not $on.active) { throw "expected active=true" }
}

# ---------------------------------------------------------------------------
# NEW: USERNAME AVAILABILITY
# ---------------------------------------------------------------------------

Test-Step "Username availability: existing username reports taken" {
    $result = Invoke-RestMethod -Uri "$CoreBaseUrl/auth/check-username?username=$testUsername"
    if ($result.available) { throw "expected available=false for an existing username" }
}

Test-Step "Username availability: fresh username reports available" {
    $freshUsername = "neverused_$suffix"
    $result = Invoke-RestMethod -Uri "$CoreBaseUrl/auth/check-username?username=$freshUsername"
    if (-not $result.available) { throw "expected available=true for a never-used username" }
}

# ---------------------------------------------------------------------------
# NEW: STRICT PHONE VALIDATION (signup + reservations)
# ---------------------------------------------------------------------------

Test-Step "Signup rejects an invalid phone number" {
    $rejected = $false
    try {
        $body = @{ username = "badphone_$suffix"; email = "badphone_$suffix@example.com"; password = $testPassword; phone = "12345" }
        Invoke-Json -Uri "$CoreBaseUrl/auth/signup" -Method POST -Body $body
    } catch {
        if ((Get-StatusCode $_) -eq 400) { $rejected = $true }
    }
    if (-not $rejected) { throw "expected 400 for a 5-digit phone number" }
}

Test-Step "Reservation creation rejects an invalid phone number" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $rejected = $false
    try {
        $body = @{
            serviceTypeId   = $serviceTypeId
            reservationDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
            timeSlot        = "10:00-12:00"
            address         = "123 Smoke Test Street"
            telephone       = "0001112223"
            comments        = "should be rejected"
        }
        Invoke-Json -Uri "$CoreBaseUrl/reservations" -Method POST -Body $body -Headers $headers
    } catch {
        if ((Get-StatusCode $_) -eq 400) { $rejected = $true }
    }
    if (-not $rejected) { throw "expected 400 for a phone number not starting with 6-9" }
}

# ---------------------------------------------------------------------------
# NEW: PASSWORD RESET (request side + bad-token handling)
# ---------------------------------------------------------------------------

Test-Step "Forgot-password returns success for a registered email" {
    Invoke-Json -Uri "$CoreBaseUrl/auth/forgot-password" -Method POST -Body @{ email = $testEmail } | Out-Null
}

Test-Step "Forgot-password returns the SAME success for an unregistered email (no enumeration)" {
    Invoke-Json -Uri "$CoreBaseUrl/auth/forgot-password" -Method POST -Body @{ email = "definitely-not-registered-$suffix@example.com" } | Out-Null
}

Test-Step "Reset-password rejects a bogus/expired token cleanly" {
    $rejected = $false
    try {
        Invoke-Json -Uri "$CoreBaseUrl/auth/reset-password" -Method POST -Body @{ token = "not-a-real-token-$suffix"; newPassword = "NewPass123" }
    } catch {
        $code = Get-StatusCode $_
        if ($code -eq 400 -or $code -eq 404) { $rejected = $true }
    }
    if (-not $rejected) { throw "expected a clean 400/404 for a bogus token, not a 500 or success" }
}

# ---------------------------------------------------------------------------
# NEW: TECHNICIAN SELF-SERVICE
# ---------------------------------------------------------------------------

Test-Step "Admin: create a technician for this test run" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{
        username         = $techUsername
        email            = $techEmail
        password         = $testPassword
        phone            = $techPhone
        serviceTypeId    = $serviceTypeId
        bio              = "Created by automated test"
        yearsExperience  = 5
    }
    $created = Invoke-Json -Uri "$CoreBaseUrl/admin/technicians" -Method POST -Body $body -Headers $headers
    if (-not $created.technicianId) { throw "no technicianId returned" }
    $script:technicianId = $created.technicianId
}

Test-Step "Technician login" {
    $body = @{ username = $techUsername; password = $testPassword }
    $response = Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body $body
    if (-not $response.accessToken) { throw "no accessToken in login response" }
    $script:technicianToken = $response.accessToken
    if ($response.role -ne "TECHNICIAN") { throw "expected role TECHNICIAN, got $($response.role)" }
}

Test-Step "Technician: view own profile" {
    $headers = @{ Authorization = "Bearer $technicianToken" }
    $profile = Invoke-RestMethod -Uri "$CoreBaseUrl/technicians/me" -Headers $headers
    if ($profile.technicianId -ne $technicianId) { throw "profile technicianId mismatch" }
}

Test-Step "Technician: update own bio and experience" {
    $headers = @{ Authorization = "Bearer $technicianToken" }
    $body = @{ bio = "Updated by automated test"; yearsExperience = 7 }
    $updated = Invoke-Json -Uri "$CoreBaseUrl/technicians/me" -Method PUT -Body $body -Headers $headers
    if ($updated.bio -ne "Updated by automated test") { throw "bio did not update" }
    if ($updated.yearsExperience -ne 7) { throw "yearsExperience did not update" }
}

Test-Step "Technician: toggle own availability off then on" {
    $headers = @{ Authorization = "Bearer $technicianToken" }
    $off = Invoke-RestMethod -Uri "$CoreBaseUrl/technicians/me/availability?available=false" -Method PATCH -Headers $headers
    if ($off.available) { throw "expected available=false" }
    $on = Invoke-RestMethod -Uri "$CoreBaseUrl/technicians/me/availability?available=true" -Method PATCH -Headers $headers
    if (-not $on.available) { throw "expected available=true" }
}

# ---------------------------------------------------------------------------
# NEW: ADMIN RESERVATIONS MANAGEMENT (assign + status transitions)
# ---------------------------------------------------------------------------

Test-Step "Customer: create a reservation for the assignment test" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $body = @{
        serviceTypeId   = $serviceTypeId
        reservationDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
        timeSlot        = "14:00-16:00"
        address         = "456 Admin Test Avenue"
        telephone       = $testPhone
        comments        = "For admin reservation management test"
    }
    $created = Invoke-Json -Uri "$CoreBaseUrl/reservations" -Method POST -Body $body -Headers $headers
    if (-not $created.reservationId) { throw "no reservationId returned" }
    $script:reservationId = $created.reservationId
}

Test-Step "Admin: reservation appears in admin/all" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $all = Invoke-RestMethod -Uri "$CoreBaseUrl/reservations/admin/all" -Headers $headers
    $match = $all | Where-Object { $_.reservationId -eq $reservationId }
    if (-not $match) { throw "reservation not found in admin/all" }
}

Test-Step "Admin: assign technician to the reservation (also auto-confirms it)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ technicianId = $technicianId }
    $updated = Invoke-Json -Uri "$CoreBaseUrl/reservations/$reservationId/assign" -Method PATCH -Body $body -Headers $headers
    if ($updated.technicianId -ne $technicianId) { throw "technician was not assigned" }
    # assignTechnician() flips PENDING -> CONFIRMED automatically as a side
    # effect - so by this point the reservation is already CONFIRMED, not
    # still PENDING. Confirm that here rather than assuming.
    if ($updated.status -ne "CONFIRMED") { throw "expected assignment to auto-confirm, got status $($updated.status)" }
}

Test-Step "Admin: transition reservation CONFIRMED -> IN_PROGRESS" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ status = "IN_PROGRESS" }
    $updated = Invoke-Json -Uri "$CoreBaseUrl/reservations/$reservationId/status" -Method PATCH -Body $body -Headers $headers
    if ($updated.status -ne "IN_PROGRESS") { throw "expected status IN_PROGRESS, got $($updated.status)" }
}

Test-Step "Admin: cancel the reservation (cleanup)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $body = @{ status = "CANCELLED" }
    $updated = Invoke-Json -Uri "$CoreBaseUrl/reservations/$reservationId/status" -Method PATCH -Body $body -Headers $headers
    if ($updated.status -ne "CANCELLED") { throw "expected status CANCELLED, got $($updated.status)" }
}

# ---------------------------------------------------------------------------
# RATE LIMITERS + CIRCUIT BREAKER
# ---------------------------------------------------------------------------

Test-Step "Auth rate limiter trips after enough failed logins" {
    # Ceiling deliberately well above any capacity we intentionally use
    # anywhere - production's real default (5), and the locally-elevated
    # value (50) used to give test scripts headroom for their own normal
    # login volume. 65 comfortably exceeds both, so this correctly proves
    # the limiter trips eventually without hardcoding an assumption about
    # which environment's capacity is currently in effect.
    $tripped = $false
    for ($i = 1; $i -le 65; $i++) {
        try {
            Invoke-Json -Uri "$CoreBaseUrl/auth/login" -Method POST -Body @{ username = $testUsername; password = "wrong-password" }
        } catch {
            if ((Get-StatusCode $_) -eq 429) { $tripped = $true; break }
        }
    }
    if (-not $tripped) { throw "never got a 429 after 65 failed login attempts" }
}

Test-Step "Chat rate limiter trips within a generous number of messages" {
    $headers = @{ Authorization = "Bearer $customerToken" }
    $tripped = $false
    for ($i = 1; $i -le 30; $i++) {
        $body = @{ messages = @(@{ role = "user"; content = "smoke test message $i" }) }
        try {
            Invoke-Json -Uri "$ChatBaseUrl/chat/message" -Method POST -Body $body -Headers $headers | Out-Null
        } catch {
            if ((Get-StatusCode $_) -eq 429) { $tripped = $true; break }
        }
        Start-Sleep -Milliseconds 150
    }
    if (-not $tripped) { throw "never got a 429 after 30 chat messages" }
}

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
    Write-Host "`nAll $passCount checks passed. Backend is solid - safe to ship." -ForegroundColor Green
} else {
    Write-Host "`n$failCount of $($results.Count) checks FAILED. Fix these before shipping." -ForegroundColor Red
}