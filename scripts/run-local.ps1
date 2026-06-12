# Charge .env.local et demarre le backend YowPainter
$envFile = Join-Path (Join-Path $PSScriptRoot "..") ".env.local"
if (-not (Test-Path $envFile)) {
    Write-Error "Fichier .env.local introuvable. Copiez .env.example vers .env.local"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
}

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

Write-Host "Kernel: $env:KSM_KERNEL_BASE_URL"
Write-Host "Backend PORT: $env:PORT"
mvn spring-boot:run
