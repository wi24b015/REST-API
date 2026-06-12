@echo off
setlocal

set "PROJECT=%~dp0"

echo == Starte Docker Infrastruktur ==
pushd "%PROJECT%docker"
docker compose up -d
if errorlevel 1 (
    echo Docker konnte nicht gestartet werden.
    pause
    exit /b 1
)
popd

echo == Starte API in neuem Terminal ==
start "energyapi" /D "%PROJECT%energyapi" cmd /k mvn spring-boot:run

echo Warte 15 Sekunden auf API-Start...
timeout /t 15 /nobreak > nul

echo == Starte Producer in neuem Terminal ==
start "energy-producer" /D "%PROJECT%energy-producer" cmd /k mvn spring-boot:run

echo == Starte User in neuem Terminal ==
start "energy-user" /D "%PROJECT%energy-user" cmd /k mvn spring-boot:run

echo Warte 10 Sekunden auf erste Messages...
timeout /t 10 /nobreak > nul

echo == Starte Usage Service in neuem Terminal ==
start "usage-service" /D "%PROJECT%usage-service" cmd /k mvn spring-boot:run

echo == Starte Current Percentage Service in neuem Terminal ==
start "current-percentage-service" /D "%PROJECT%current-percentage-service" cmd /k mvn spring-boot:run

echo == Starte GUI in neuem Terminal ==
start "energy-gui" /D "%PROJECT%GUI" cmd /k mvn javafx:run

echo.
echo Live-Umgebung wurde gestartet.
echo In der GUI kannst du refresh und show data testen.
echo Dieses Fenster kann geschlossen werden.

endlocal