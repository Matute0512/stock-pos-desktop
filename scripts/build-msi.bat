@echo off
setlocal

set APP_NAME=Stock POS
set APP_VERSION=1.0.0
set VENDOR=Tu Nombre
set DESCRIPTION=Sistema de stock y punto de venta
set MAIN_CLASS=com.local.pos.Main
set MAIN_JAR=stock-pos-desktop-1.0.0.jar
set ICON=src\main\resources\images\icon.ico
set STAGE=target\stage
set UPGRADE_UUID=bdf4e4bc-5a22-464d-bbfd-f699deb6a5a5

echo ============================================================
echo   Stock POS - Generador de instalador MSI (empresarial)
echo ============================================================

echo.
echo [1/5] Ejecutando mvn clean package...
call mvn clean package
if errorlevel 1 (
    echo [ERROR] Falló mvn clean package. Abortando.
    exit /b 1
)
echo [OK] Compilación y empaquetado exitoso.

set JAR=target\%MAIN_JAR%
if not exist "%JAR%" (
    echo [ERROR] No se encontró el JAR: %JAR%
    exit /b 1
)
echo [OK] JAR encontrado: %JAR%

set JPACKAGE=%JAVA_HOME%\bin\jpackage.exe
if not exist "%JPACKAGE%" (
    echo [ERROR] No se encontró jpackage.
    echo         Verificá que JAVA_HOME apunte a un JDK 21 (no un JRE).
    exit /b 1
)
echo [OK] jpackage: %JPACKAGE%

set ICON_ARGS=
if exist "%ICON%" (
    set ICON_ARGS=--icon "%ICON%"
    echo [OK] Ícono encontrado: %ICON%
) else (
    echo [AVISO] No hay ícono. Se usará el default de jpackage.
)

echo.
echo [2/5] Preparando directorio de staging...
if exist "%STAGE%" rmdir /s /q "%STAGE%"
mkdir "%STAGE%" >nul
copy "%JAR%" "%STAGE%\" >nul
xcopy "target\dependency" "%STAGE%\dependency\" /e /i /y >nul
echo [OK] Staging listo en %STAGE%

echo.
echo [3/5] Ejecutando jpackage --type msi (requiere WiX 3.11)...
mkdir target\installer 2>nul

"%JPACKAGE%" ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --vendor "%VENDOR%" ^
    --description "%DESCRIPTION%" ^
    --input "%STAGE%" ^
    --main-jar %MAIN_JAR% ^
    --main-class %MAIN_CLASS% ^
    --type msi ^
    --win-upgrade-uuid %UPGRADE_UUID% ^
    --win-menu ^
    --win-shortcut ^
    %ICON_ARGS% ^
    --dest target\installer

if errorlevel 1 (
    echo [ERROR] Falló jpackage. Si el error menciona WiX,
    echo         instalá WiX Toolset 3.11 para poder generar el .msi.
    exit /b 1
)

echo.
echo [4/5] Verificando instalador generado...
dir "target\installer\*.msi"

echo.
echo [5/5] Listo. El instalador MSI está en: target\installer\
echo.
endlocal
