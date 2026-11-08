# Builds the mod and copies the jar into the FTB Evolution 2 dev clone (replaces only our own old jars).
# Usage: powershell -NoProfile -File tools\deploy-to-test-instance.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$instance = 'C:\Users\oli\curseforge\minecraft\Instances\FTB Evolution 2 - Dev EASYNPC catcher'
$mods = Join-Path $instance 'mods'
if (-not (Test-Path $mods)) { throw "Instance mods folder not found: $mods" }

$env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\openjdk-25.0.1'
Push-Location $root
try {
  cmd /c ".\gradlew.bat build --console=plain 2>&1" | Select-String -Pattern 'Fehler|error:|BUILD' | Select-Object -Last 5
  if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
} finally { Pop-Location }

$jar = Get-ChildItem (Join-Path $root 'build\libs') -Filter 'easy_npc_trading_station-*.jar' |
  Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem $mods -Filter 'easy_npc_trading_station*.jar' | Remove-Item -Force
Copy-Item $jar.FullName $mods
"Deployed $($jar.Name) -> $mods"
