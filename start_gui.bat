@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==================================================
echo   DANG BIEN DICH CODE (UTF-8)...
echo ==================================================
javac -encoding UTF-8 -d out src\server\*.java src\client\*.java
if %errorlevel% neq 0 (
    echo.
    echo [LOI] Bien dich that bai! Kiem tra lai ma nguon.
    pause
    exit /b
)
echo.
echo [OK] Bien dich thanh cong! Dang khoi dong GUI Client...
echo ==================================================
echo.
java -cp out client.LoginFrame
pause
