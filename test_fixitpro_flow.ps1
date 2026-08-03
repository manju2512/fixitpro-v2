# FixitPro v2 - end-to-end test flow (PowerShell version)
# Run with: docker compose up --build -d   (then, from this folder)  .\test_fixitpro_flow.ps1
# For CI, or if you've changed the seeded admin password from the default,
# pass it explicitly: .\test_fixitpro_flow.ps1 -AdminPassword "..."
param(
    [string]$AdminPassword = "ChangeMe123"
)
$ErrorActionPreference = "Stop"
$BASE = "http://localhost:8080/api"

function Invoke-Json($Method, $Path, $Body = $null, $Token = $null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $uri = "$BASE$Path"
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body ($Body | ConvertTo-Json)
    } else {
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    }
}

Write-Host "== 1. Admin login =="
$adminAuth = Invoke-Json POST "/auth/login" @{ username = "admin"; password = $AdminPassword }
$ADMIN_TOKEN = $adminAuth.accessToken
Write-Host "Admin token acquired: $($ADMIN_TOKEN.Substring(0,20))..."

Write-Host "== 2. Create a technician (Electrician, serviceTypeId=1) =="
$techBody = @{
    username = "tech_raj"; email = "raj@fixitpro.local"; password = "TechPass123"
    phone = "9990001111"; serviceTypeId = 1; bio = "10 yrs residential wiring"; yearsExperience = 10
}
try {
    $tech = Invoke-Json POST "/admin/technicians" $techBody $ADMIN_TOKEN
    $tech | ConvertTo-Json
    $TECH_ID = $tech.technicianId
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "tech_raj already exists from a previous run - reusing it"
        $existing = Invoke-Json GET "/admin/technicians" $null $ADMIN_TOKEN
        $match = $existing | Where-Object { $_.name -eq "tech_raj" } | Select-Object -First 1
        $TECH_ID = $match.technicianId
        Write-Host "Reusing technicianId $TECH_ID"
    } else { throw }
}

Write-Host "== 3. Sign up a customer =="
try {
    $signup = Invoke-Json POST "/auth/signup" @{ username = "cust_amit"; email = "amit@example.com"; password = "CustPass123"; phone = "9998887777" }
    $signup | ConvertTo-Json
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "cust_amit already exists from a previous run - skipping signup"
    } else { throw }
}

Write-Host "-- Ensuring cust_amit is active (a previous interrupted run may have left it deactivated) --"
$custCheck = Invoke-Json GET "/admin/users?role=CUSTOMER" $null $ADMIN_TOKEN
$existingCust = $custCheck | Where-Object { $_.username -eq "cust_amit" } | Select-Object -First 1
if ($existingCust -and -not $existingCust.active) {
    Invoke-Json PATCH "/admin/users/$($existingCust.userId)/status?active=true" $null $ADMIN_TOKEN | Out-Null
    Write-Host "cust_amit was deactivated from a previous run - reactivated"
}

Write-Host "== 4. Customer login =="
# The customer's password may have been changed to CustPassNew456 by step 14 in a prior run.
try {
    $custAuth = Invoke-Json POST "/auth/login" @{ username = "cust_amit"; password = "CustPass123" }
    $CUST_CURRENT_PASSWORD = "CustPass123"
} catch {
    $custAuth = Invoke-Json POST "/auth/login" @{ username = "cust_amit"; password = "CustPassNew456" }
    $CUST_CURRENT_PASSWORD = "CustPassNew456"
    Write-Host "(original password didn't work - logged in with the password from a previous run's step 14 instead)"
}
$CUST_TOKEN = $custAuth.accessToken
Write-Host "Customer token acquired: $($CUST_TOKEN.Substring(0,20))..."

Write-Host "== 5. List service types & technicians =="
(Invoke-Json GET "/service-types") | ConvertTo-Json
(Invoke-Json GET "/technicians?serviceTypeId=1") | ConvertTo-Json

Write-Host "== 6. Book a reservation (explicit technician -> starts PENDING) =="
$resBody = @{
    serviceTypeId = 1; technicianId = $TECH_ID
    reservationDate = (Get-Date).AddDays(3).ToString("yyyy-MM-dd"); timeSlot = "10:00-12:00"
    address = "12 MG Road, Bengaluru"; telephone = "9998887777"; comments = "Fan not working"
}
$res = Invoke-Json POST "/reservations" $resBody $CUST_TOKEN
$res | ConvertTo-Json
$RES_ID = $res.reservationId

Write-Host "== 7. Admin walks the status lifecycle: CONFIRMED -> IN_PROGRESS -> COMPLETED =="
(Invoke-Json PATCH "/reservations/$RES_ID/status" @{ status = "CONFIRMED" } $ADMIN_TOKEN) | ConvertTo-Json
(Invoke-Json PATCH "/reservations/$RES_ID/status" @{ status = "IN_PROGRESS" } $ADMIN_TOKEN) | ConvertTo-Json
(Invoke-Json PATCH "/reservations/$RES_ID/status" @{ status = "COMPLETED" } $ADMIN_TOKEN) | ConvertTo-Json

Write-Host "== 8. Customer leaves a review =="
$review = Invoke-Json POST "/reviews" @{ reservationId = $RES_ID; rating = 5; comment = "Great job, fixed it fast" } $CUST_TOKEN
$review | ConvertTo-Json
$REVIEW_ID = $review.reviewId

Write-Host "== 9. Technician replies to the review =="
$techAuth = Invoke-Json POST "/auth/login" @{ username = "tech_raj"; password = "TechPass123" }
$TECH_TOKEN = $techAuth.accessToken
$reply = Invoke-Json POST "/reviews/$REVIEW_ID/reply" @{ replyText = "Thanks for the kind words!" } $TECH_TOKEN
$reply | ConvertTo-Json
$REPLY_ID = $reply.reply.replyId

Write-Host "== 10. Admin moderates the reply (HIDDEN) =="
(Invoke-Json PATCH "/admin/reviews/replies/$REPLY_ID/moderate" @{ status = "HIDDEN" } $ADMIN_TOKEN) | ConvertTo-Json

Write-Host "== 11. Admin user management =="
(Invoke-Json GET "/admin/users?role=CUSTOMER" $null $ADMIN_TOKEN) | ConvertTo-Json

$adminList = Invoke-Json GET "/admin/users?role=ADMIN" $null $ADMIN_TOKEN
$ADMIN_ID = $adminList[0].userId
Write-Host "-- Attempting self-deactivation (should fail, 4xx) --"
try {
    Invoke-Json PATCH "/admin/users/$ADMIN_ID/status?active=false" $null $ADMIN_TOKEN
} catch {
    Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__)"
}

$custList = Invoke-Json GET "/admin/users?role=CUSTOMER" $null $ADMIN_TOKEN
$CUST_ID = $custList[0].userId
Write-Host "-- Deactivating the customer account --"
Invoke-Json PATCH "/admin/users/$CUST_ID/status?active=false" $null $ADMIN_TOKEN
Write-Host "-- Verify deactivated customer can no longer log in (should fail, 401) --"
try {
    Invoke-Json POST "/auth/login" @{ username = "cust_amit"; password = $CUST_CURRENT_PASSWORD } | Out-Null
    Write-Host "UNEXPECTED: deactivated customer was still able to log in"
} catch {
    Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__) (deactivated account rejected)"
}
Write-Host "-- Reactivating the customer account (later steps need cust_amit to log in) --"
Invoke-Json PATCH "/admin/users/$CUST_ID/status?active=true" $null $ADMIN_TOKEN

Write-Host "== 12. Dashboard stats =="
(Invoke-Json GET "/admin/dashboard/stats" $null $ADMIN_TOKEN) | ConvertTo-Json

Write-Host "== 13. Admin lists all technicians (including unavailable) =="
(Invoke-Json GET "/admin/technicians" $null $ADMIN_TOKEN) | ConvertTo-Json

Write-Host "== 14. Password change (self-service) =="
if ($CUST_CURRENT_PASSWORD -eq "CustPassNew456") {
    Write-Host "-- Password was already changed to CustPassNew456 in a previous run - skipping the change, just re-verifying --"
} else {
    Write-Host "-- Customer changes own password --"
    Invoke-Json PATCH "/users/me/password" @{ currentPassword = "CustPass123"; newPassword = "CustPassNew456" } $CUST_TOKEN
    Write-Host "-- Verify old password now fails --"
    try {
        Invoke-Json POST "/auth/login" @{ username = "cust_amit"; password = "CustPass123" }
        Write-Host "UNEXPECTED: old password still works"
    } catch {
        Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__) (old password rejected)"
    }
}
Write-Host "-- Verify new password works --"
$reAuth = Invoke-Json POST "/auth/login" @{ username = "cust_amit"; password = "CustPassNew456" }
$CUST_TOKEN = $reAuth.accessToken
Write-Host "Re-authenticated with new password: $($CUST_TOKEN.Substring(0,20))..."

Write-Host "== 15. Reservation cancellation rules =="
Write-Host "-- Book a second reservation (explicit technician) to cancel --"
$resBody2 = @{
    serviceTypeId = 1; technicianId = $TECH_ID
    reservationDate = (Get-Date).AddDays(7).ToString("yyyy-MM-dd"); timeSlot = "14:00-16:00"
    address = "45 Brigade Road, Bengaluru"; telephone = "9998887777"; comments = "Socket sparking"
}
$res2 = Invoke-Json POST "/reservations" $resBody2 $CUST_TOKEN
$RES2_ID = $res2.reservationId
Write-Host "Created reservation $RES2_ID with status $($res2.status)"

Write-Host "-- Technician attempts to cancel it (should fail, 403) --"
try {
    Invoke-Json PATCH "/reservations/$RES2_ID/status" @{ status = "CANCELLED" } $TECH_TOKEN
    Write-Host "UNEXPECTED: technician was able to cancel"
} catch {
    Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__) (technician cannot cancel)"
}

Write-Host "-- Customer attempts to jump straight to COMPLETED (should fail, invalid transition) --"
try {
    Invoke-Json PATCH "/reservations/$RES2_ID/status" @{ status = "COMPLETED" } $CUST_TOKEN
    Write-Host "UNEXPECTED: customer was able to set a non-cancel status"
} catch {
    Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__) (customers can only cancel)"
}

Write-Host "-- Customer cancels their own reservation (should succeed) --"
$cancelled = Invoke-Json PATCH "/reservations/$RES2_ID/status" @{ status = "CANCELLED" } $CUST_TOKEN
$cancelled | ConvertTo-Json

Write-Host "-- Attempting to move a CANCELLED (terminal) reservation forward (should fail) --"
try {
    Invoke-Json PATCH "/reservations/$RES2_ID/status" @{ status = "CONFIRMED" } $ADMIN_TOKEN
    Write-Host "UNEXPECTED: terminal reservation was moved"
} catch {
    Write-Host "Got expected error: $($_.Exception.Response.StatusCode.value__) (terminal state is final)"
}

Write-Host "== 16. Auto-assignment path (no technicianId given) =="
$resBody3 = @{
    serviceTypeId = 1
    reservationDate = (Get-Date).AddDays(8).ToString("yyyy-MM-dd"); timeSlot = "09:00-11:00"
    address = "78 Indiranagar, Bengaluru"; telephone = "9998887777"; comments = "New wiring for AC unit"
}
$res3 = Invoke-Json POST "/reservations" $resBody3 $CUST_TOKEN
$res3 | ConvertTo-Json
if ($res3.status -eq "CONFIRMED" -and $res3.technicianId) {
    Write-Host "PASS: auto-assigned technician $($res3.technicianName), status starts CONFIRMED (as designed - an available technician existed)"
} elseif ($res3.status -eq "PENDING" -and -not $res3.technicianId) {
    Write-Host "PASS (no technician was available): booking still went through as PENDING, unassigned"
} else {
    Write-Host "UNEXPECTED combination - status: $($res3.status), technicianId: $($res3.technicianId)"
}

Write-Host "== Done =="