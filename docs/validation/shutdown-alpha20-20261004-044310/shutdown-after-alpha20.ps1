param([switch] $VerifyOnly)
$ErrorActionPreference = 'Stop'
$deliveryDirectory = $PSScriptRoot
$manifestPath = Join-Path $deliveryDirectory 'manifest.json'
$shutdownLog = Join-Path $deliveryDirectory 'shutdown.log'
$receiptPath = Join-Path $deliveryDirectory 'shutdown-receipt.json'
$expectedVersion = '0.3.0-native-alpha.20'
$expectedHash = '28656ccfc2c9d87067b0e9f27647132b5db80bc760886e2327d88b316e5022d7'
function Write-ShutdownLog([string] $message) {
    Add-Content -LiteralPath $shutdownLog -Encoding UTF8 -Value ((Get-Date).ToUniversalTime().ToString('o') + ' ' + $message)
}
function Assert-ArtifactHash([string] $artifactPath, [string] $digest) {
    $artifactStream = [System.IO.File]::OpenRead($artifactPath)
    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    try { $actual = [BitConverter]::ToString($algorithm.ComputeHash($artifactStream)).Replace('-', '').ToLowerInvariant() }
    finally { $artifactStream.Dispose(); $algorithm.Dispose() }
    if ($actual -ne $digest.ToLowerInvariant()) { throw 'Artifact hash mismatch.' }
}
function Assert-Delivery {
    $delivery = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($delivery.completed -ne $true -or $delivery.version -ne $expectedVersion -or $delivery.build_succeeded -ne $true -or @($delivery.audit_errors).Count -ne 0) { throw 'Build/audit incomplete.' }
    if ($delivery.sha256 -ne $expectedHash -or $delivery.bytes -ne 56178050 -or $delivery.all_production_class_resource_bytes_identical_to_alpha19 -ne $true) { throw 'Unexpected release identity.' }
    Assert-ArtifactHash $delivery.jar $expectedHash
    if ((Get-Item -LiteralPath $delivery.jar).Length -ne $delivery.bytes) { throw 'Release size mismatch.' }
    Assert-ArtifactHash $delivery.build_log $delivery.build_log_sha256
    if ((Get-Content -LiteralPath $delivery.build_log -Raw -Encoding UTF8) -notmatch 'BUILD SUCCESSFUL') { throw 'Build success marker missing.' }
    Assert-ArtifactHash $delivery.regression_evidence.log $delivery.regression_evidence.sha256
    if ((Get-Content -LiteralPath $delivery.regression_evidence.log -Raw -Encoding UTF8) -notmatch 'All 288 required tests passed') { throw 'Prior regression success missing.' }
    $todoPath = [System.IO.Path]::GetFullPath((Join-Path $deliveryDirectory '..\..\..\..\MAINLINE_GAMEPLAY_PARITY_TODO.md'))
    $label = [string]::Concat([char]0x9636, [char]0x6BB5, ' 91')
    if ((Get-Content -LiteralPath $todoPath -Raw -Encoding UTF8) -notmatch $label) { throw 'Final TODO stage missing.' }
    return $delivery
}
try {
    $verifiedDelivery = Assert-Delivery
    if ($VerifyOnly) { Write-ShutdownLog 'Preflight passed; no shutdown request in VerifyOnly mode.'; Write-Output 'Verified'; exit 0 }
    Write-ShutdownLog 'Explicit user request; release, successful build, audit and saved TODO verified. Waiting20 seconds before normal shutdown.'
    $receipt = [ordered]@{version=$expectedVersion;sha256=$expectedHash;helper_pid=$PID;verified_at_utc=(Get-Date).ToUniversalTime().ToString('o');delay_seconds=20;shutdown_command='shutdown.exe /s /t 0';status='verified_waiting'}
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    Start-Sleep -Seconds 20
    $verifiedDelivery = Assert-Delivery
    $receipt.status = 'requesting_shutdown'
    $receipt.requested_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    & "$env:SystemRoot\System32\shutdown.exe" /s /t 0
    $code = $LASTEXITCODE
    Write-ShutdownLog "shutdown.exe exit code: $code"
    $receipt.shutdown_exit_code = $code
    $receipt.status = if ($code -eq 0) { 'shutdown_requested' } else { 'shutdown_request_failed' }
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    exit $code
} catch {
    Write-ShutdownLog ('Shutdown cancelled due to validation/helper error: ' + $_.Exception.Message)
    exit 1
}
