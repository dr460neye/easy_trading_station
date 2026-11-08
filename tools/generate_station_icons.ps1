# Generates 16x16 inventory icons and flat item models for all trading station variants.
# Usage: powershell -File tools\generate_station_icons.ps1
# Output: src/main/resources/assets/easy_npc_trading_station/{textures,models}/item/station_<variant>.*
Add-Type -AssemblyName System.Drawing

$root = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $root 'src\main\resources\assets\easy_npc_trading_station'
$texDir = Join-Path $assets 'textures\item'
$modelDir = Join-Path $assets 'models\item'
$itemDefDir = Join-Path $assets 'items'
New-Item -ItemType Directory -Force $texDir, $modelDir, $itemDefDir | Out-Null

function C($r, $g, $b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }
function Shade($c, $f) {
  [System.Drawing.Color]::FromArgb(255,
    [Math]::Min(255, [int]($c.R * $f)), [Math]::Min(255, [int]($c.G * $f)), [Math]::Min(255, [int]($c.B * $f)))
}

# variant -> base material color ('futuristic' is special-cased)
$variants = [ordered]@{
  futuristic = C 55 65 90
  stone      = C 125 125 125
  oak        = C 162 130 78
  spruce     = C 104 78 47
  birch      = C 196 179 123
  jungle     = C 160 115 80
  acacia     = C 168 90 50
  dark_oak   = C 66 43 20
  mangrove   = C 117 54 48
  cherry     = C 226 178 172
  bamboo     = C 193 173 80
  crimson    = C 112 49 70
  warped     = C 43 104 99
}

foreach ($name in $variants.Keys) {
  $base = $variants[$name]
  $futuristic = $name -eq 'futuristic'
  $bmp = New-Object System.Drawing.Bitmap 16, 16

  $frame = if ($futuristic) { C 40 48 70 } else { Shade $base 0.55 }
  $glass = if ($futuristic) { C 120 200 255 } else { C 205 228 238 }
  $glassD = if ($futuristic) { C 90 170 235 } else { C 175 205 218 }
  $glow = if ($futuristic) { C 150 240 255 } else { C 245 250 252 }
  $skin = C 224 172 125; $hair = C 80 50 30; $eye = C 30 30 30; $shirt = C 60 160 90

  # glass body (x 1..14, y 1..10) with outer frame
  for ($y = 1; $y -le 10; $y++) {
    for ($x = 1; $x -le 14; $x++) {
      $edge = ($x -in 1, 14) -or ($y -in 1, 10)
      $c = if ($edge) { $frame } elseif ((($x + $y) % 5) -eq 0) { $glow } else { if ((($x + $y) % 2) -eq 0) { $glass } else { $glassD } }
      $bmp.SetPixel($x, $y, $c)
    }
  }
  # access window frame (x 4..11, y 3..8), glass stays inside
  for ($x = 4; $x -le 11; $x++) { $bmp.SetPixel($x, 3, $frame); $bmp.SetPixel($x, 8, $frame) }
  for ($y = 3; $y -le 8; $y++) { $bmp.SetPixel(4, $y, $frame); $bmp.SetPixel(11, $y, $frame) }
  for ($y = 4; $y -le 7; $y++) { for ($x = 5; $x -le 10; $x++) { $bmp.SetPixel($x, $y, $(if ((($x + $y) % 2) -eq 0) { $glass } else { $glassD })) } }

  # NPC behind the window: head 2x2, body 2x2
  foreach ($p in @(@(7, 4), @(8, 4), @(7, 5), @(8, 5))) { $bmp.SetPixel($p[0], $p[1], $skin) }
  $bmp.SetPixel(7, 4, $hair); $bmp.SetPixel(8, 4, $hair); $bmp.SetPixel(7, 5, $eye); $bmp.SetPixel(8, 5, $eye)
  foreach ($p in @(@(7, 6), @(8, 6), @(7, 7), @(8, 7))) { $bmp.SetPixel($p[0], $p[1], $shirt) }

  # base (x 1..14, y 11..14)
  for ($y = 11; $y -le 14; $y++) {
    for ($x = 1; $x -le 14; $x++) {
      $c = $base
      if ($y -eq 14) { $c = Shade $base 0.7 }
      elseif ($futuristic -and $y -eq 11) { $c = $glow }
      elseif ($name -eq 'stone') { if ((($x * 3 + $y * 5) % 7) -eq 0) { $c = Shade $base 0.8 } }
      elseif (-not $futuristic) { if ($y -eq 12 -or $y -eq 13) { if ((($x + $y) % 6) -eq 0) { $c = Shade $base 0.8 } } }
      if ($x -in 1, 14) { $c = Shade $c 0.75 }
      $bmp.SetPixel($x, $y, $c)
    }
  }

  $bmp.Save((Join-Path $texDir "station_$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()

  $model = "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": {`n    `"layer0`": `"easy_npc_trading_station:item/station_$name`"`n  }`n}`n"
  [IO.File]::WriteAllText((Join-Path $modelDir "station_$name.json"), $model, (New-Object Text.UTF8Encoding $false))

  # MC 26.1 needs a client item definition (assets/<ns>/items/<id>.json) for every item.
  $def = "{`n  `"model`": {`n    `"type`": `"minecraft:model`",`n    `"model`": `"easy_npc_trading_station:item/station_$name`"`n  }`n}`n"
  [IO.File]::WriteAllText((Join-Path $itemDefDir "station_$name.json"), $def, (New-Object Text.UTF8Encoding $false))
}
"Generated $($variants.Count) icons in $texDir"
