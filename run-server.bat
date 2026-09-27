@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
if errorlevel 1 pause & exit /b 1
java -cp "out;lib/*" quiz.Server 5050 data
pause
