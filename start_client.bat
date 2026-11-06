@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==================================================
echo   DANG BIEN DICH TOAN BO HE THONG (UTF-8)...
echo ==================================================
javac -encoding UTF-8 -cp "lib/*" -d out src\server\*.java src\server\db\*.java src\server\model\*.java src\server\dao\*.java src\server\util\*.java src\client\*.java
if %errorlevel% neq 0 (
    echo.
    echo [LOI] Bien dich that bai! Kiem tra lai ma nguon.
    pause
    exit /b
)
echo.
echo ==================================================
echo   DANG KHOI DONG FTP CLIENT (CLI)...
echo ==================================================
echo.
java -cp "out;lib/*" client.TCPClient %*
pause
