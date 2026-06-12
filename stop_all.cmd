@echo off
setlocal

set "PROJECT=%~dp0"

echo == Stoppe Spring Boot Prozesse aus diesem Projekt ==
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$project = '%PROJECT:\=\\%';" ^
  "$processes = Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine -like '*semesterprojekt*' -and ($_.CommandLine -like '*spring-boot:run*' -or $_.CommandLine -like '*javafx:run*') };" ^
  "foreach ($process in $processes) { Write-Host ('Stoppe PID ' + $process.ProcessId + ': ' + $process.Name); Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue }"

echo == Prüfe Port 8080 ==
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "ABH"') do (
    echo Stoppe Prozess auf Port 8080: %%a
    taskkill /PID %%a /F >nul 2>nul
)

echo Fertig.
endlocal
