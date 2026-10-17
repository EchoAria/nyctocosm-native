param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    if (-not (Test-Path -LiteralPath 'run/saves/NativeSmoke/level.dat')) { throw 'Create a disposable NativeSmoke world first. This test modifies that world.' }
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $env:JAVA_TOOL_OPTIONS = '-Dnet.minecraftforge.gradle.check.certs=false'
    $optionsPath = Join-Path $projectRoot 'run/options.txt'
    $originalOptions = [System.IO.File]::ReadAllText($optionsPath)
    try {
        & .\gradlew.bat runClient -PresonanceSmoke --offline *> docs/resonance-client.log
        if ($LASTEXITCODE -ne 0) { throw 'Client failed; see docs/resonance-client.log' }
        if (-not (Select-String -LiteralPath docs/resonance-client.log -Pattern 'RESONANCE_SMOKE_COMPLETE frames=341 sounds=14' -Quiet)) { throw 'Client exited without completing the capture and sound checks.' }
    } finally { [System.IO.File]::WriteAllText($optionsPath, $originalOptions) }
} finally { Pop-Location }
