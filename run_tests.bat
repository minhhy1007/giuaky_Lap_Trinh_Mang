@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ==================================================
echo   DANG BIEN DICH TOAN BO HE THONG VA TESTS...
echo ==================================================
javac -encoding UTF-8 -cp "lib/*" -d out src\server\*.java src\server\db\*.java src\server\model\*.java src\server\dao\*.java src\server\util\*.java src\client\*.java test\*.java
if %errorlevel% neq 0 (
    echo.
    echo [LOI] Bien dich that bai!
    pause
    exit /b
)
echo.
echo ==================================================
echo   1. CHAY TEST PHASE 1 (DATABASE & JDBC)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase1Database
echo.
echo ==================================================
echo   2. CHAY TEST PHASE 2 (AUTH & USER SANDBOX)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase2AuthAndSandbox
echo.
echo ==================================================
echo   3. CHAY TEST PHASE 3 (STORAGE, QUOTA & INTEGRITY)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase3StorageAndQuota
echo.
echo ==================================================
echo   4. CHAY TEST PHASE 4 (SHARING, ACCESS VA AUDIT)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase4SharingAndHistory
echo.
echo ==================================================
echo   5. CHAY TEST PHASE 5 (PLANS, ORDERS VA PAYMENTS)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase5PlansAndPayment
echo.
echo ==================================================
echo   6. CHAY TEST PHASE 6 (ADMIN DASHBOARD & UI/UX)...
echo ==================================================
java -cp "out;lib/*" test.TestPhase6AdminDashboard
echo.
echo ==================================================
echo   TAT CA TEST CUA 6 PHASE DA HOAN TAT 100%!
echo ==================================================
pause
