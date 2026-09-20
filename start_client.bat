@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==================================================
echo   DANG KHOI DONG FTP CLIENT...
echo ==================================================
echo.
java -cp out client.TCPClient
pause
