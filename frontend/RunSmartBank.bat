
@echo off
cd /d "%~dp0\.."

echo Starting SmartBank backend...
javac MAIN.java ApiServer.java

if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)

start "SmartBank Backend" cmd /k "cd /d ""%~dp0\.."" && java ApiServer"

timeout /t 3 /nobreak >nul
start "" "http://127.0.0.1:5500/index.html"

pause
