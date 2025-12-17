# =============================================================================
# BUILD SKRIPT FÜR AI4MBSE PLUGIN
# =============================================================================

# 1. PFADE KONFIGURIEREN

# A) Wo liegen die Programm-Dateien (zum Lesen der Bibliotheken)?
$programDir = "C:\Program Files\Magic Systems of Systems Architect"

# B) Wo soll das Plugin installiert werden (AppData)?
$pluginInstallBase = "C:\Users\simon\AppData\Local\.magic.systems.of.systems.architect\2024x\plugins"
$pluginInstallDir  = "$pluginInstallBase\AI4MBSE_valido"

# C) Java Tools
$javaPath  = "C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot"
$jarTool   = "$javaPath\bin\jar.exe"
$javacTool = "$javaPath\bin\javac.exe"


# 2. CHECKS
# Lib-Ordner prüfen
if (-not (Test-Path "$programDir\lib\md.jar")) {
    Write-Host "FEHLER: 'lib' Ordner nicht gefunden in: $programDir" -ForegroundColor Red
    exit 1
} else {
    Write-Host "Lese Bibliotheken aus: $programDir" -ForegroundColor Green
}

# Java prüfen
try { & $javacTool -version | Out-Null } catch {
    Write-Error "Java JDK nicht gefunden! Bitte Pfad prüfen."; exit 1
}


# 3. PROJEKT-PFADE SETZEN
$projectRoot    = $PSScriptRoot
$srcFolder      = "$projectRoot\src\main\java"
$resourceFolder = "$projectRoot\src\main\resources"
$localLibFolder = "$projectRoot\lib"
$buildFolder    = "$projectRoot\target\classes"
$outputJar      = "$projectRoot\target\ai4mbse-plugin.jar"


# 4. AUFRÄUMEN
Write-Host "Bereite Build vor..."
if (Test-Path $buildFolder) { Remove-Item -Recurse -Force $buildFolder }
New-Item -ItemType Directory -Path $buildFolder | Out-Null
if (-not (Test-Path "$projectRoot\target")) { New-Item -ItemType Directory -Path "$projectRoot\target" | Out-Null }


# 5. KOMPILIEREN
Write-Host "Kompiliere..."

# Classpath: Programm-Libs + Lokale Libs
$classpath = "$programDir\lib\*;$localLibFolder\*"

# Dateien finden
$javaFiles = Get-ChildItem -Recurse "$srcFolder\*.java" | Select-Object -ExpandProperty FullName
if (-not $javaFiles) { Write-Error "Keine Java-Dateien gefunden!"; exit 1 }

# Liste schreiben (ohne BOM Fehler)
[System.IO.File]::WriteAllLines("$projectRoot\target\sources.txt", $javaFiles)

# Compiler starten
& $javacTool -encoding UTF-8 -cp "$classpath" -d "$buildFolder" "@$projectRoot\target\sources.txt"

if ($LASTEXITCODE -ne 0) { Write-Error "Kompilierung fehlgeschlagen!"; exit 1 }


# 6. RESSOURCEN KOPIEREN
if (Test-Path $resourceFolder) {
    Copy-Item "$resourceFolder\*" -Destination $buildFolder -Recurse -Force
}


# 7. JAR PACKEN
Write-Host "Erstelle JAR..."
& $jarTool -cf "$outputJar" -C "$buildFolder" .

if ($LASTEXITCODE -ne 0) { Write-Error "JAR Erstellung fehlgeschlagen!"; exit 1 }
Write-Host "BUILD ERFOLGREICH: $outputJar" -ForegroundColor Green


# 8. INSTALLIEREN (In den AppData Ordner)
Write-Host "Installiere Plugin..."
try {
    # Plugin-Ordner erstellen, falls nicht vorhanden
    if (-not (Test-Path $pluginInstallDir)) {
        New-Item -ItemType Directory -Path $pluginInstallDir -Force | Out-Null
    }

    # JAR kopieren
    Copy-Item "$outputJar" -Destination "$pluginInstallDir" -Force

    # plugin.xml kopieren (Sicherheitshalber)
    if (Test-Path "$resourceFolder\plugin.xml") {
        Copy-Item "$resourceFolder\plugin.xml" -Destination "$pluginInstallDir" -Force
    }

    Write-Host "Installiert nach: $pluginInstallDir" -ForegroundColor Cyan
    Write-Host "Bitte Catia neu starten! Das Plugin sollte jetzt geladen werden." -ForegroundColor Yellow

} catch {
    Write-Error "Konnte nicht in den AppData-Ordner schreiben: $_"
}