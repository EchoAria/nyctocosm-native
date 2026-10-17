$ErrorActionPreference = 'Stop'
$deliveryDirectory = $PSScriptRoot
$manifestPath = Join-Path $deliveryDirectory 'manifest.json'
$shutdownLog = Join-Path $deliveryDirectory 'shutdown.log'
$receiptPath = Join-Path $deliveryDirectory 'shutdown-receipt.json'
$expectedVersion = '0.3.0-native-alpha.17'
$expectedJarHash = '84c4f1ce6a1f862665ab49cdf56c03cd7f10deda0a3a2845adae410fc9c271f5'

function Write-ShutdownLog([string] $message) {
    Add-Content -LiteralPath $shutdownLog -Encoding UTF8 -Value ((Get-Date).ToUniversalTime().ToString('o') + ' ' + $message)
}

function Assert-ArtifactHash([string] $artifactPath, [string] $expectedHash) {
    if (-not (Test-Path -LiteralPath $artifactPath -PathType Leaf)) {
        throw "Missing artifact: $artifactPath"
    }
    $artifactStream = [System.IO.File]::OpenRead($artifactPath)
    $sha256Algorithm = [System.Security.Cryptography.SHA256]::Create()
    try {
        $actualHash = [BitConverter]::ToString($sha256Algorithm.ComputeHash($artifactStream)).Replace('-', '').ToLowerInvariant()
    } finally {
        $artifactStream.Dispose()
        $sha256Algorithm.Dispose()
    }
    if ($actualHash -ne $expectedHash.ToLowerInvariant()) {
        throw "Artifact SHA256 mismatch: $artifactPath"
    }
}

function Assert-Delivery {
    $delivery = Get-Content -LiteralPath $manifestPath -Encoding UTF8 -Raw | ConvertFrom-Json
    if ($delivery.completed -ne $true -or $delivery.version -ne $expectedVersion) {
        throw 'Delivery is incomplete or version is incorrect.'
    }
    if ($delivery.sha256 -ne $expectedJarHash -or $delivery.bytes -ne 56065908) {
        throw 'Unexpected release identity.'
    }
    if ($delivery.tests.required_passed -ne 262 -or @($delivery.audit.errors).Count -ne 0) {
        throw 'Required tests or artifact audit did not pass.'
    }
    Assert-ArtifactHash $delivery.jar $expectedJarHash
    if ((Get-Item -LiteralPath $delivery.jar).Length -ne $delivery.bytes) {
        throw 'Release size mismatch.'
    }
    Assert-ArtifactHash $delivery.tests.log $delivery.tests.sha256
    Assert-ArtifactHash $delivery.release_build.log $delivery.release_build.sha256
    $testLog = Get-Content -LiteralPath $delivery.tests.log -Encoding UTF8 -Raw
    $buildLog = Get-Content -LiteralPath $delivery.release_build.log -Encoding UTF8 -Raw
    if ($testLog -notmatch 'All 262 required tests passed' -or $testLog -notmatch 'BUILD SUCCESSFUL' -or $buildLog -notmatch 'BUILD SUCCESSFUL') {
        throw 'Final test/build success markers are missing.'
    }
    if (@($delivery.client_runs).Count -ne 3) {
        throw 'Expected three client validation runs.'
    }
    foreach ($clientRun in $delivery.client_runs) {
        Assert-ArtifactHash $clientRun.log $clientRun.sha256
        if ((Get-Content -LiteralPath $clientRun.log -Encoding UTF8 -Raw) -notmatch 'VISUAL_REPAIR_CAPTURE_COMPLETE') {
            throw "Client completion marker is missing: $($clientRun.log)"
        }
    }
    $reportPath = Join-Path (Split-Path -Parent $deliveryDirectory) 'FEEDBACK_REPAIR_ALPHA17.md'
    if (-not (Test-Path -LiteralPath $reportPath -PathType Leaf)) {
        throw 'Delivery report is missing.'
    }
    $todoPath = [System.IO.Path]::GetFullPath((Join-Path $deliveryDirectory '..\..\..\..\MAINLINE_GAMEPLAY_PARITY_TODO.md'))
    $finalStageLabel = [string]::Concat([char]0x9636, [char]0x6BB5, ' 83')
    if ((Get-Content -LiteralPath $todoPath -Encoding UTF8 -Raw) -notmatch $finalStageLabel) {
        throw 'Final TODO stage is missing.'
    }
    return $delivery
}

try {
    Write-ShutdownLog 'Started; explicit user request: shut down after repairs and successful build. Graceful shutdown without /f.'
    $verifiedDelivery = Assert-Delivery
    $receipt = [ordered]@{
        version = $expectedVersion
        jar_sha256 = $expectedJarHash
        verified_at_utc = (Get-Date).ToUniversalTime().ToString('o')
        helper_pid = $PID
        required_tests_passed = 262
        delay_seconds = 20
        shutdown_command = 'shutdown.exe /s /t 0'
        status = 'verified_waiting'
    }
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    Write-ShutdownLog 'Package SHA256, audit, final test/build logs, three client logs and saved TODO verified; waiting 20 seconds.'
    Start-Sleep -Seconds 20
    $verifiedDelivery = Assert-Delivery
    $receipt.status = 'requesting_shutdown'
    $receipt.requested_at_utc = (Get-Date).ToUniversalTime().ToString('o')
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    Write-ShutdownLog 'Requesting normal shutdown: shutdown.exe /s /t 0'
    & "$env:SystemRoot\System32\shutdown.exe" /s /t 0
    $shutdownExit = $LASTEXITCODE
    Write-ShutdownLog "shutdown.exe returned exit code $shutdownExit"
    $receipt.shutdown_exit_code = $shutdownExit
    $receipt.status = if ($shutdownExit -eq 0) { 'shutdown_requested' } else { 'shutdown_request_failed' }
    $receipt | ConvertTo-Json | Set-Content -LiteralPath $receiptPath -Encoding UTF8
    exit $shutdownExit
} catch {
    Write-ShutdownLog ('Shutdown cancelled due to validation/helper error: ' + $_.Exception.Message)
    exit 1
}
