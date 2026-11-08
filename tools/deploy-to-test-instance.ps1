# Builds the mod and copies the jar into a local test instance (replaces only our own old jars).
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy-to-test-instance.ps1
# Needs tools\local.json (copy tools\local.example.json): testInstance, optional javaHome.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$configPath = Join-Path $PSScriptRoot 'local.json'
if (-not (Test-Path $configPath)) { throw "Missing tools\local.json - copy tools\local.example.json and adapt it" }
$config = Get-Content $configPath -Raw | ConvertFrom-Json
$mods = Join-Path $config.testInstance 'mods'
if (-not (Test-Path $mods)) { throw "Instance mods folder not found: $mods" }
if ($config.javaHome) { $env:JAVA_HOME = $config.javaHome }

Push-Location $root
try {
  cmd /c ".\gradlew.bat build --console=plain 2>&1" | Select-String -Pattern 'Fehler|error:|BUILD' | Select-Object -Last 5
  if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
} finally { Pop-Location }

$jar = Get-ChildItem (Join-Path $root 'build\libs') -Filter 'easy-trading-station-neoforge-*.jar' |
  Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw 'No jar found in build\libs' }
# also removes jars of the former name (easy_npc_trading_station-*.jar)
Get-ChildItem $mods -Filter '*trading_station*.jar' | Remove-Item -Force
Get-ChildItem $mods -Filter 'easy-trading-station*.jar' | Remove-Item -Force
Copy-Item $jar.FullName $mods
"Deployed $($jar.Name) -> $mods"
