@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
if errorlevel 1 pause & exit /b 1

set SERVER_IP=%1
if "%SERVER_IP%"=="" (
    echo ========================================================
    echo             QUIZ ARENA - CLIENT CONNECTION
    echo ========================================================
    echo * Neu choi tren cung may voi Server: Chi can an phim ENTER
    echo * Neu choi tu may khac trong mang: Nhap IP may chu (VD: 192.168.1.131)
    echo ========================================================
    set /p SERVER_IP="Nhap IP Server (an Enter de mac dinh 127.0.0.1): "
)
if "%SERVER_IP%"=="" set SERVER_IP=127.0.0.1

set SERVER_PORT=%2
if "%SERVER_PORT%"=="" set SERVER_PORT=5050

echo Dang ket noi toi Server: %SERVER_IP%:%SERVER_PORT%...
java -cp "out;lib/*" quiz.Client %SERVER_IP% %SERVER_PORT%
pause
