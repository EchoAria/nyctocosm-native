$ErrorActionPreference = 'Stop'
$receiptPath = Join-Path $PSScriptRoot 'SHUTDOWN_BUILD_ALPHA16_20261002-080804.json'
$receipt = Get-Content -LiteralPath $receiptPath -Raw -Encoding utf8 | ConvertFrom-Json
if (-not $receipt.build_succeeded -or @($receipt.audit_errors).Count -ne 0) { throw 'Build or audit did not succeed; shutdown canceled.' }
$artifactHash = (Get-FileHash -LiteralPath $receipt.jar -Algorithm SHA256).Hash
if ($artifactHash -ne $receipt.sha256) { throw 'Artifact changed; shutdown canceled.' }
Start-Sleep -Seconds 20
$shutdownExe = Join-Path $env:SystemRoot 'System32/shutdown.exe'
& $shutdownExe /s /t 0
$shutdownResult = $LASTEXITCODE
Set-Content -LiteralPath (Join-Path $PSScriptRoot 'shutdown-request-alpha16-20261002-080804.log') -Encoding utf8 -Value "Windows shutdown request exit code: $shutdownResult"
exit $shutdownResult
