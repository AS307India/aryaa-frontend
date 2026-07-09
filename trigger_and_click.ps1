$adb = "C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "1. Clearing logcat..."
& $adb logcat -c

Write-Host "2. Running the test to trigger notification in background..."
Start-Process -NoNewWindow -FilePath ".\gradlew.bat" -ArgumentList "connectedAndroidTest `"-Pandroid.testInstrumentationRunnerArguments.class=com.as307.aryaa.service.SosTriggerTest`""

Write-Host "3. Waiting for SosService to start by polling logcat..."
$started = $false
for ($i = 0; $i -lt 40; $i++) {
    $logs = & $adb logcat -d -s SOS_NOTIF:D *:S
    if ($logs -match "onStartCommand") {
        $started = $true
        Write-Host "SosService started!"
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $started) {
    Write-Error "SosService failed to start within time limit!"
    exit 1
}

# Wait for notification to be fully posted and rendered
Start-Sleep -Seconds 5

Write-Host "4. Expanding notifications panel..."
& $adb shell cmd statusbar expand-notifications
Start-Sleep -Seconds 3

Write-Host "5. Dumping UI hierarchy..."
& $adb shell uiautomator dump /sdcard/window_dump.xml
Start-Sleep -Seconds 1
& $adb pull /sdcard/window_dump.xml D:\AS307\window_dump.xml

Write-Host "6. Parsing XML to find 'I'm Safe' bounds..."
$content = Get-Content D:\AS307\window_dump.xml -Raw
if ($content -match 'text="I''m Safe"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') {
    $x1 = [int]$Matches[1]
    $y1 = [int]$Matches[2]
    $x2 = [int]$Matches[3]
    $y2 = [int]$Matches[4]
    
    $centerX = [int](($x1 + $x2) / 2)
    $centerY = [int](($y1 + $y2) / 2)
    
    Write-Host "Found 'I'm Safe' button at bounds: [$x1,$y1][$x2,$y2]. Center: ($centerX, $centerY)"
    
    Write-Host "7. Tapping the button..."
    & $adb shell input tap $centerX $centerY
} else {
    Write-Warning "Could not find 'I'm Safe' text in window_dump.xml!"
}

Start-Sleep -Seconds 4

Write-Host "8. Pulling SOS_NOTIF logs..."
& $adb logcat -d -s SOS_NOTIF:D *:S
