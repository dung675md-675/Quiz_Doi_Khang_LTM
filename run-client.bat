@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
if errorlevel 1 pause & exit /b 1

set SERVER_IP=%1
if "%SERVER_IP%"=="" (
    echo ========================================================
    echo             QUIZ ARENA - CLIENT CONNECTION
    echo ========================================================
    set /p SERVER_IP="Nhap IP Server (an Enter de mac dinh 127.0.0.1): "
)
if "%SERVER_IP%"=="" set SERVER_IP=127.0.0.1

set SERVER_PORT=%2
if "%SERVER_PORT%"=="" set SERVER_PORT=5050

echo Dang ket noi toi Server: %SERVER_IP%:%SERVER_PORT%...
java -cp out quiz.Client %SERVER_IP% %SERVER_PORT%
pause
