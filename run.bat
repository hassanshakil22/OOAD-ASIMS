@echo off
REM Starts ASIMS. Then open http://localhost:8000 in your browser.
call "%~dp0mvnw.cmd" -q compile exec:java
