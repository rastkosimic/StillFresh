#Requires -Version 5.1
<#
.SYNOPSIS
  Generate root init-*.sql files from db-init/templates using values from .env.

.DESCRIPTION
  Postgres init scripts cannot read environment variables. This script substitutes
  __POSTGRES_PASSWORD_*__ placeholders before docker-compose mounts the SQL files.

  Existing Postgres volumes keep their original passwords until recreated
  (docker-compose down -v). Re-run this script whenever you change DB passwords in .env.
#>
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$EnvFile = Join-Path $Root ".env"
$TemplateDir = Join-Path $Root "db-init\templates"

if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing .env at $EnvFile. Copy .env.example to .env and fill in values first."
}

# Parse KEY=VALUE from .env (skip comments / blank lines)
$envMap = @{}
Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $idx = $line.IndexOf("=")
    if ($idx -lt 1) { return }
    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1)
    # Strip surrounding quotes
    if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
        $val = $val.Substring(1, $val.Length - 2)
    }
    $envMap[$key] = $val
}

function Escape-SqlLiteral([string]$value) {
    if ($null -eq $value) { return "" }
    return $value.Replace("'", "''")
}

function Require-Env([string]$name) {
    if (-not $envMap.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($envMap[$name])) {
        Write-Error "Required variable $name is missing or empty in .env"
    }
    return (Escape-SqlLiteral $envMap[$name])
}

$replacements = @{
    "__POSTGRES_PASSWORD_USER__"         = (Require-Env "POSTGRES_PASSWORD_USER")
    "__POSTGRES_PASSWORD_VENDOR__"       = (Require-Env "POSTGRES_PASSWORD_VENDOR")
    "__POSTGRES_PASSWORD_AUTH__"         = (Require-Env "POSTGRES_PASSWORD_AUTH")
    "__POSTGRES_PASSWORD_OFFERS__"       = (Require-Env "POSTGRES_PASSWORD_OFFERS")
    "__POSTGRES_PASSWORD_ORDERS__"       = (Require-Env "POSTGRES_PASSWORD_ORDERS")
    "__POSTGRES_PASSWORD_PAYMENTS__"     = (Require-Env "POSTGRES_PASSWORD_PAYMENTS")
    "__POSTGRES_PASSWORD_NOTIFICATION__" = (Require-Env "POSTGRES_PASSWORD_NOTIFICATION")
}

$templates = @(
    "init-user.sql.template",
    "init-vendor.sql.template",
    "init-auth.sql.template",
    "init-offer.sql.template",
    "init-order.sql.template",
    "init-payment.sql.template",
    "init-notification.sql.template"
)

foreach ($tmplName in $templates) {
    $tmplPath = Join-Path $TemplateDir $tmplName
    if (-not (Test-Path $tmplPath)) {
        Write-Error "Template not found: $tmplPath"
    }
    $content = Get-Content -Raw -Path $tmplPath
    foreach ($key in $replacements.Keys) {
        $content = $content.Replace($key, $replacements[$key])
    }
    if ($content -match "__POSTGRES_PASSWORD_") {
        Write-Error "Unresolved password placeholder remaining in $tmplName"
    }
    $outName = $tmplName -replace "\.template$", ""
    $outPath = Join-Path $Root $outName
    # UTF-8 without BOM for Postgres
    [System.IO.File]::WriteAllText($outPath, $content)
    Write-Host "Wrote $outName"
}

Write-Host "Done. Generated init-*.sql are gitignored; commit only db-init/templates/."
