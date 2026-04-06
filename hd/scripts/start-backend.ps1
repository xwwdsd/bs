$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Resolve-Path (Join-Path $scriptDir "..")
Set-Location $projectRoot

# Fixed environment variables required by backend startup.
$env:JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
$env:STEAM_API_KEY = "dev-steam-api-key"

Write-Host "Starting backend in $projectRoot ..."
Write-Host "JWT_SECRET and STEAM_API_KEY are loaded from script."

mvn spring-boot:run
