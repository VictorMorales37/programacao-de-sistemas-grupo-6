@echo off
cd /d "%~dp0"
java -jar dist\sic-xe-simulator.jar
if errorlevel 1 pause
