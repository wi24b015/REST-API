@echo off
setlocal

set "PROJECT=%~dp0"

echo == Raeume alte Projekt-Prozesse auf ==
call "%PROJECT%stop_all.cmd"

echo == Pruefe Docker Infrastruktur ==
pushd "%PROJECT%docker"

call :ensure_compose_service database
if errorlevel 1 (
echo Docker database konnte nicht gestartet werden.
popd
pause
exit /b 1
)

echo Starte database neu, um alte DB-Verbindungen zu schliessen ...
docker compose restart database
if errorlevel 1 (
echo Docker database konnte nicht neu gestartet werden.
popd
pause
exit /b 1
)

echo Warte 8 Sekunden auf PostgreSQL...
timeout /t 8 /nobreak > nul

call :ensure_compose_service rabbitmq
if errorlevel 1 (
echo Docker rabbitmq konnte nicht gestartet werden.
popd
pause
exit /b 1
)

echo Warte 5 Sekunden auf RabbitMQ...
timeout /t 5 /nobreak > nul

docker compose ps
popd

echo.
echo == Starte Usage Service in neuem Terminal ==
call :start_maven_app "usage-service" "%PROJECT%usage-service" "clean spring-boot:run"

echo Warte 15 Sekunden auf Usage Service und Flyway Migration...
timeout /t 15 /nobreak > nul

echo.
echo == Starte Current Percentage Service in neuem Terminal ==
call :start_maven_app "current-percentage-service" "%PROJECT%current-percentage-service" "clean spring-boot:run"

echo Warte 8 Sekunden auf Current Percentage Service...
timeout /t 8 /nobreak > nul

echo.
echo == Starte API in neuem Terminal ==
call :start_maven_app "energyapi" "%PROJECT%energyapi" "clean spring-boot:run"

echo Warte 10 Sekunden auf API-Start...
timeout /t 10 /nobreak > nul

echo.
echo == Starte Producer in neuem Terminal ==
call :start_maven_app "energy-producer" "%PROJECT%energy-producer" "clean spring-boot:run"

echo Warte 3 Sekunden...
timeout /t 3 /nobreak > nul

echo.
echo == Starte User in neuem Terminal ==
call :start_maven_app "energy-user" "%PROJECT%energy-user" "clean spring-boot:run"

echo Warte 10 Sekunden auf erste Messages...
timeout /t 10 /nobreak > nul

echo.
echo == Starte GUI in neuem Terminal ==
call :start_maven_app "energy-gui" "%PROJECT%GUI" "clean javafx:run"

echo.
echo ============================================
echo Live-Umgebung wurde gestartet.
echo RabbitMQ UI: http://localhost:15672
echo REST API:    http://localhost:8080/energy/current
echo GUI:         eigenes Fenster
echo ============================================
echo.
echo Dieses Fenster kann geschlossen werden.
pause

goto :finish

:ensure_compose_service
set "SERVICE=%~1"
set "CONTAINER_ID="
set "IS_RUNNING="

for /f "delims=" %%i in ('docker compose ps -q %SERVICE%') do set "CONTAINER_ID=%%i"

if defined CONTAINER_ID (
for /f "delims=" %%s in ('docker inspect -f "{{.State.Running}}" %CONTAINER_ID% 2^>nul') do set "IS_RUNNING=%%s"
if /I "%IS_RUNNING%"=="true" (
echo %SERVICE% laeuft bereits.
exit /b 0
)
)

echo Starte %SERVICE% ...
docker compose up -d %SERVICE%
exit /b %errorlevel%

:start_maven_app
set "WINDOW_TITLE=%~1"
set "APP_DIR=%~2"
set "MAVEN_GOAL=%~3"

if exist "%APP_DIR%\mvnw.cmd" (
start "%WINDOW_TITLE%" /D "%APP_DIR%" cmd /k mvnw.cmd %MAVEN_GOAL%
) else (
start "%WINDOW_TITLE%" /D "%APP_DIR%" cmd /k mvn %MAVEN_GOAL%
)

exit /b 0

:finish
endlocal
