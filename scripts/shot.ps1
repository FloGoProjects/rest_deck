# Screenshot vom angeschlossenen Handy. Aufruf: .\scripts\shot.ps1 <name>  -> build\shots\<name>.png
param([string]$Name = "shot")
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$dir = Join-Path $PSScriptRoot "..\build\shots"
New-Item -ItemType Directory -Force $dir | Out-Null
& $adb shell screencap -p /sdcard/rd_shot.png
& $adb pull /sdcard/rd_shot.png (Join-Path $dir "$Name.png") | Out-Null
& $adb shell rm /sdcard/rd_shot.png
Resolve-Path (Join-Path $dir "$Name.png")
