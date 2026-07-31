# Reads the active ngrok tunnel (local inspector API) and updates ALLSECURE_PUBLIC_BASE_URL in .env.
# Prerequisite: ngrok must be running, e.g.  ngrok http 8080
#
# Usage (from repo root):
#   .\scripts\update-allsecure-public-url.ps1
#   docker-compose up -d --build payment-service

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envFile = Join-Path $repoRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile — copy .env.example to .env first."
}

try {
    $tunnels = Invoke-RestMethod -Uri "http://127.0.0.1:4040/api/tunnels" -Method Get
} catch {
    Write-Error "Cannot reach ngrok inspector at http://127.0.0.1:4040 — start ngrok first:  ngrok http 8080"
}

$httpsUrl = $tunnels.tunnels |
    Where-Object { $_.public_url -like "https://*" } |
    Select-Object -First 1 -ExpandProperty public_url

if (-not $httpsUrl) {
    Write-Error "No https tunnel found. Run:  ngrok http 8080"
}

$httpsUrl = $httpsUrl.TrimEnd("/")
Write-Host "Detected ngrok URL: $httpsUrl"

$content = Get-Content $envFile -Raw
if ($content -match "(?m)^ALLSECURE_PUBLIC_BASE_URL=.*$") {
    $content = $content -replace "(?m)^ALLSECURE_PUBLIC_BASE_URL=.*$", "ALLSECURE_PUBLIC_BASE_URL=$httpsUrl"
} else {
    $content += "`nALLSECURE_PUBLIC_BASE_URL=$httpsUrl`n"
}
Set-Content -Path $envFile -Value $content -NoNewline

Write-Host "Updated ALLSECURE_PUBLIC_BASE_URL in .env"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. docker-compose up -d --build payment-service"
Write-Host "  2. POST /payment/allsecure/register-card  (get a NEW redirectUrl)"
Write-Host "  3. Complete card entry + 3DS (Authenticated ECI 05)"
