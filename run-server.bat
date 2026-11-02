@echo off
cd /d "%~dp0"
where javac >nul 2>nul || (if exist "D:\jdk21\bin\javac.exe" set "PATH=D:\jdk21\bin;%PATH%")
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
if errorlevel 1 pause & exit /b 1
java -cp "out;lib/*" quiz.Server 5050 data
pause
