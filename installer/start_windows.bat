@echo off
TITLE Valido Installer

:: 1. In das Verzeichnis wechseln
cd /d "%~dp0"

echo ---------------------------------------
echo Starte Valido Installer...
echo ---------------------------------------

:: 2. Java pruefen
java -version >nul 2>&1
if %errorlevel% neq 0 goto ERROR_JAVA

:: 3. Wenn Java da ist -> Starten
echo Java gefunden. Starte Anwendung...
java -jar Valido_Installer.jar
goto ENDE

:ERROR_JAVA
echo.
echo [FEHLER] Java wurde nicht gefunden!
echo.
echo Der Installer benoetigt Java.
echo Bitte laden Sie es hier herunter und installieren Sie es:
echo.
echo Empfohlene Version: JDK 17 (LTS)
echo LINK: https://adoptium.net/de/temurin/releases/
echo.
echo (Nach der Installation dieses Fenster schliessen und neu starten)
goto ENDE

:ENDE
echo.
echo Druecken Sie eine Taste zum Beenden...
pause