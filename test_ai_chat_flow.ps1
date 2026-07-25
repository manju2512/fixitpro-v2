# FixitPro v2 - AI chat assistant test flow
# Requires a REAL GROQ_API_KEY set in .env (free, no credit card - get one at
# https://console.groq.com). Run: docker compose up --build -d after setting
# it. Uses the same customer created by test_fixitpro_flow.ps1 if you've
# already run that; otherwise signs one up fresh.
#
# Exits non-zero on any real failure (unconfigured key, tool-use not firing,
# no reservation actually created) so this is safe to gate CI on, not just a
# script that prints warnings nobody reads.
#
# Run with: powershell -ExecutionPolicy Bypass -File .\test_ai_chat_flow.ps1
$ErrorActionPreference = "Stop"
$CORE = "http://localhost:8080/api"
$CHAT = "http://localhost:8081/api"

function Invoke-Json($Method, $BaseUrl, $Path, $Body = $null, $Token = $null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $uri = "$BaseUrl$Path"
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body ($Body | ConvertTo-Json -Depth 10)
    } else {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    }
}

function Assert-Configured($response) {
    if ($response.reply -match "(?i)isn't set up yet|not configured|GROQ_API_KEY") {
        Write-Host "FAIL: assistant reports it isn't configured - GROQ_API_KEY is missing or invalid." -ForegroundColor Red
        exit 1
    }
}

Write-Host "== 1. Get a customer token (reuses cust_amit if present, signs up fresh otherwise) =="
try {
    $auth = Invoke-Json POST $CORE "/auth/login" @{ username = "cust_amit"; password = "CustPass123" }
} catch {
    try {
        $auth = Invoke-Json POST $CORE "/auth/login" @{ username = "cust_amit"; password = "CustPassNew456" }
    } catch {
        $auth = Invoke-Json POST $CORE "/auth/signup" @{ username = "cust_amit"; email = "amit@example.com"; password = "CustPass123"; phone = "9998887777" }
    }
}
$TOKEN = $auth.accessToken
Write-Host "Customer token acquired: $($TOKEN.Substring(0,20))..."

Write-Host "== 2. Ask the assistant what services are offered (should trigger list_service_types) =="
$history = @(@{ role = "user"; content = "Hi! What services do you offer?" })
$resp1 = Invoke-Json POST $CHAT "/chat/message" @{ messages = $history } $TOKEN
Write-Host "Assistant: $($resp1.reply)"
Assert-Configured $resp1
if ($resp1.reply -notmatch "(?i)electric|plumb|carpent") {
    Write-Host "FAIL: reply didn't mention any known service type - list_service_types likely wasn't called." -ForegroundColor Red
    exit 1
}

Write-Host "== 3. Ask it to book something, providing all details up front =="
$history = $resp1.messages + @(@{
    role = "user"
    content = "Please book an electrician for tomorrow, 09:00-11:00, at 12 MG Road, Bengaluru, phone 9998887777. It's a broken switch."
})
$resp2 = Invoke-Json POST $CHAT "/chat/message" @{ messages = $history } $TOKEN
Write-Host "Assistant: $($resp2.reply)"
Assert-Configured $resp2

Write-Host "== 4. Verify a real reservation actually got created via core-service directly =="
$reservations = Invoke-Json GET $CORE "/reservations/me" $null $TOKEN
$latest = $reservations | Sort-Object reservationId -Descending | Select-Object -First 1
if ($latest -and $latest.comments -match "(?i)switch") {
    Write-Host "PASS: found reservation #$($latest.reservationId), status $($latest.status), for $($latest.serviceTypeName) on $($latest.reservationDate) $($latest.timeSlot)" -ForegroundColor Green
} else {
    Write-Host "FAIL: no matching reservation found - the tool call likely didn't complete. Latest reservations:" -ForegroundColor Red
    $reservations | Format-Table reservationId, serviceTypeName, reservationDate, timeSlot, status, comments
    exit 1
}

Write-Host "== 5. Ask about existing bookings (should trigger list_my_reservations) =="
$history = $resp2.messages + @(@{ role = "user"; content = "What bookings do I have right now?" })
$resp3 = Invoke-Json POST $CHAT "/chat/message" @{ messages = $history } $TOKEN
Write-Host "Assistant: $($resp3.reply)"
Assert-Configured $resp3
if ($resp3.reply -notmatch "(?i)#$($latest.reservationId)|switch|electric") {
    Write-Host "FAIL: reply didn't reference the actual booking - list_my_reservations likely wasn't called correctly." -ForegroundColor Red
    exit 1
}

Write-Host "== Done - all AI chat checks passed ==" -ForegroundColor Green
