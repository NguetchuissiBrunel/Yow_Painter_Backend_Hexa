# Cree la ClientApplication "yowpainter-backend" dans le kernel local.
# Pre-requis : kernel sur http://localhost:8080 avec bootstrap platform-admin actif.

param(
    [string]$KernelBaseUrl = "http://localhost:8080",
    [string]$TenantId = "11111111-1111-1111-1111-111111111111",
    [string]$BootstrapClientId = "dev-platform-backend",
    [string]$BootstrapApiKey = "dev-api-key",
    [string]$YowPainterClientId = "yowpainter-backend",
    [string]$YowPainterClientSecret = "yowpainter-local-dev-secret-2026"
)

$ErrorActionPreference = "Stop"

function Invoke-KernelJson {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body = $null
    )
    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ContentType = "application/json"
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    return Invoke-RestMethod @params
}

$bootstrapHeaders = @{
    "X-Client-Id" = $BootstrapClientId
    "X-Api-Key"   = $BootstrapApiKey
    "X-Tenant-Id" = $TenantId
}

Write-Host "Connexion platform-admin sur $KernelBaseUrl ..."
$login = Invoke-KernelJson -Method POST -Uri "$KernelBaseUrl/api/auth/login" -Headers $bootstrapHeaders -Body @{
    principal = "platform-admin"
    password  = "281kFEOhFYuj7n9cfgGp"
}

$accessToken = $login.data.accessToken
if (-not $accessToken -and $login.data.mfaToken) {
    Write-Host "MFA requis. Confirmation avec codePreview ..."
    $mfa = Invoke-KernelJson -Method POST -Uri "$KernelBaseUrl/api/auth/login/mfa/confirm" -Headers $bootstrapHeaders -Body @{
        mfaToken = $login.data.mfaToken
        code     = $login.data.codePreview
    }
    $accessToken = $mfa.data.accessToken
}

if (-not $accessToken) {
    throw "Impossible d'obtenir un accessToken platform-admin."
}

$adminHeaders = $bootstrapHeaders.Clone()
$adminHeaders["Authorization"] = "Bearer $accessToken"

Write-Host "Creation ClientApplication $YowPainterClientId ..."
try {
    $created = Invoke-KernelJson -Method POST -Uri "$KernelBaseUrl/api/client-applications" -Headers $adminHeaders -Body @{
        clientId        = $YowPainterClientId
        name            = "YowPainter Backend"
        description     = "Backend marketplace YowPainter connecte au kernel"
        clientSecret    = $YowPainterClientSecret
        allowedServices = @(
            "ORGANIZATION",
            "SETTINGS",
            "COMMERCIAL",
            "PRODUCT",
            "SALES",
            "NOTIFICATION",
            "ADMINISTRATION"
        )
    }
    Write-Host "ClientApplication creee. clientId=$YowPainterClientId"
    if ($created.data.clientSecret) {
        Write-Host "Secret genere par le kernel (a copier dans .env.local) : $($created.data.clientSecret)"
    }
}
catch {
    $responseBody = $_.ErrorDetails.Message
    if ($responseBody -match "MFA_REQUIRED_FOR_ADMIN") {
        Write-Host ""
        Write-Host "Le compte platform-admin doit avoir le MFA active dans le kernel avant de creer une ClientApplication."
        Write-Host "Contournement dev immediat : dans .env.local, utilisez :"
        Write-Host "  KSM_KERNEL_CLIENT_ID=dev-platform-backend"
        Write-Host "  KSM_KERNEL_API_KEY=dev-api-key"
        exit 1
    }
    $status = $_.Exception.Response.StatusCode.value__
    if ($status -eq 409) {
        Write-Host "ClientApplication $YowPainterClientId existe deja. Verifiez que KSM_KERNEL_API_KEY dans .env.local correspond au secret kernel."
        exit 0
    }
    throw
}

Write-Host ""
Write-Host "Variables .env.local recommandees :"
Write-Host "KSM_KERNEL_BASE_URL=$KernelBaseUrl"
Write-Host "KSM_KERNEL_CLIENT_ID=$YowPainterClientId"
Write-Host "KSM_KERNEL_API_KEY=$YowPainterClientSecret"
Write-Host "KSM_KERNEL_TENANT_ID=$TenantId"
