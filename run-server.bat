@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -sourcepath src -d out src\quiz\*.java src\quiz\ui\*.java
if errorlevel 1 pause & exit /b 1
java -cp out quiz.Server 5050 data
pause
