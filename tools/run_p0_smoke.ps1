param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectRoot
try {
    if (-not (Test-Path -LiteralPath 'run/saves/NativeSmoke/level.dat')) {
        throw 'Create a disposable world named NativeSmoke with runClient first. This script changes that test world.'
    }
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $env:JAVA_TOOL_OPTIONS = '-Dnet.minecraftforge.gradle.check.certs=false'
    $optionsPath = Join-Path $projectRoot 'run/options.txt'
    $originalOptions = [System.IO.File]::ReadAllText($optionsPath)
    $packPath = Join-Path $projectRoot 'run/resourcepacks/p0-reference'
    New-Item -ItemType Directory -Force -Path "$packPath/assets/minecraft/models/item" | Out-Null
    [System.IO.File]::WriteAllText("$packPath/pack.mcmeta", '{"pack":{"pack_format":15,"description":"P0 development-only original helmet reference"}}')
    [System.IO.File]::WriteAllText("$packPath/assets/minecraft/models/item/glowstone_dust.json", '{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/glowstone_dust"},"overrides":[{"predicate":{"custom_model_data":19},"model":"nyctocosm:magic/sorcerer_plane"}]}')
    $testOptions = $originalOptions -replace '(?m)^resourcePacks:.*$', 'resourcePacks:["vanilla","mod_resources","file/p0-reference"]'
    [System.IO.File]::WriteAllText($optionsPath, $testOptions)
    try {
        & .\gradlew.bat runClient -Pp0Smoke --offline *> docs/p0-client-console.log
        if ($LASTEXITCODE -ne 0) { throw "Client failed; see docs/p0-client-console.log" }
        if (-not (Select-String -LiteralPath docs/p0-client-console.log -Pattern 'P0_SMOKE_COMPLETE frames=811' -Quiet)) { throw 'Client exited without completing the continuous capture.' }
    } finally {
        [System.IO.File]::WriteAllText($optionsPath, $originalOptions)
    }
} finally { Pop-Location }
