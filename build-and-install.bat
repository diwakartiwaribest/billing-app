@echo off
setlocal
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
set ANDROID_HOME=%USERPROFILE%\Android\Sdk
set PATH=%USERPROFILE%\Android\platform-tools;%PATH%

echo ====================================
echo   Building APK...
echo ====================================
call "%~dp0gradlew.bat" assembleDebug --no-daemon --quiet
if %ERRORLEVEL% neq 0 (
    echo BUILD FAILED! Check errors above.
    pause
    exit /b 1
)

set APK=%~dp0app\build\outputs\apk\debug\app-debug.apk

echo.
echo ====================================
echo   Installing on phone...
echo ====================================
adb install -r "%APK%"
if %ERRORLEVEL% neq 0 (
    echo.
    echo Install failed. Make sure:
    echo   1. Phone is connected via USB
    echo   2. USB Debugging is enabled
    echo   3. You allowed debugging on the phone popup
    pause
    exit /b 1
)

echo.
echo ====================================
echo   Launching app...
echo ====================================
adb shell am start -n com.shop.billing/.MainActivity

echo.
echo Done! App installed and launched.
pause
