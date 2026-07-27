# test-chat-rate-limit.ps1
#
# Logs in as a customer, then fires chat messages at ai-chat-service in a
# loop and reports the HTTP status of each. Expects the first
# app.rate-limit.chat.capacity (default 15) to succeed (200) and everything
# after that to return 429 until the refill window passes.
#
# USAGE:
#   .\test-chat-rate-limit.ps1
#   .\test-chat-rate-limit.ps1 -Username "yourtestuser" -Password "yourpassword" -Count 20

param(
    [string]$Username = "test",
    [string]$Password = "password123",
    [string]$CoreBaseUrl = "http://localhost:8080/api",
    [string]$ChatBaseUrl = "http://localhost:8081/api",
    [int]$Count = 20
)

Write-Host "== Logging in as '$Username' ==" -ForegroundColor Cyan
try {
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $loginResponse = Invoke-RestMethod -Uri "$CoreBaseUrl/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    $token = $loginResponse.accessToken
    if (-not $token) {
        Write-Host "Login succeeded but no accessToken in response - check AuthResponse shape." -ForegroundColor Red
        exit 1
    }
    Write-Host "Logged in OK. userId=$($loginResponse.userId) role=$($loginResponse.role)" -ForegroundColor Green
} catch {
    Write-Host "Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Check -Username/-Password params point at a real customer account." -ForegroundColor Yellow
    exit 1
}

Write-Host "`n== Sending $Count chat messages to $ChatBaseUrl/chat/message ==" -ForegroundColor Cyan

$headers = @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" }
$rateLimitHit = $false
$firstBlockedAt = $null

for ($i = 1; $i -le $Count; $i++) {
    $body = @{ messages = @(@{ role = "user"; content = "test message $i" }) } | ConvertTo-Json -Depth 5

    try {
        $response = Invoke-WebRequest -Uri "$ChatBaseUrl/chat/message" -Method POST -Headers $headers -Body $body -ErrorAction Stop
        Write-Host ("Message {0,2} -> {1}" -f $i, $response.StatusCode) -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 429) {
            if (-not $rateLimitHit) {
                $rateLimitHit = $true
                $firstBlockedAt = $i
            }
            Write-Host ("Message {0,2} -> 429 (rate limited)" -f $i) -ForegroundColor Yellow
        } else {
            Write-Host ("Message {0,2} -> {1} (unexpected)" -f $i, $statusCode) -ForegroundColor Red
        }
    }

    Start-Sleep -Milliseconds 200  # avoid tripping raw connection limits, not the rate limiter itself
}

Write-Host "`n== Result ==" -ForegroundColor Cyan
if ($rateLimitHit) {
    Write-Host "PASS: rate limiter kicked in at message #$firstBlockedAt" -ForegroundColor Green
} else {
    Write-Host "FAIL: never got a 429 in $Count attempts - rate limiter is not active." -ForegroundColor Red
    Write-Host "Check: was ai-chat-service rebuilt after adding ChatRateLimitWebFilter?" -ForegroundColor Yellow
    Write-Host "Check: does AuthWebFilter have @Order(1)?" -ForegroundColor Yellow
}
