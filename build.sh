#!/bin/bash

# =============================================================================
# BUILD SKRIPT FÜR AI4MBSE PLUGIN (MACOS)
# =============================================================================

# Farben für Output
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 1. PFADE KONFIGURIEREN (Deine Antworten)
# Der Hauptordner
PROGRAM_DIR="/Applications/Magic Systems of Systems Architect"

# Der Ordner mit den Programm-Bibliotheken (md.jar)
LIB_DIR="$PROGRAM_DIR/lib"

# Wo soll das Plugin hin? (Direkt in den App-Ordner)
# Wir hängen den Plugin-Namen an, damit ein sauberer Unterordner entsteht
PLUGIN_BASE_DIR="/Applications/Magic Systems of Systems Architect/plugins"
PLUGIN_INSTALL_DIR="$PLUGIN_BASE_DIR/AI4MBSE_valido"

# Java Tools (Systemstandard nutzen)
JAR_TOOL="jar"
JAVAC_TOOL="javac"

# 2. CHECKS
echo "--- Starte Checks ---"

# Lib-Ordner prüfen
if [ ! -f "$LIB_DIR/md.jar" ]; then
    echo -e "${RED}FEHLER: 'md.jar' nicht gefunden in: $LIB_DIR${NC}"
    exit 1
else
    echo -e "${GREEN}Bibliotheken gefunden: $LIB_DIR${NC}"
fi

# Java prüfen
if ! command -v $JAVAC_TOOL &> /dev/null; then
    echo -e "${RED}Java Compiler nicht gefunden!${NC}"
    exit 1
fi

# 3. PROJEKT-PFADE SETZEN
PROJECT_ROOT=$(pwd)
SRC_FOLDER="$PROJECT_ROOT/src/main/java"
RESOURCE_FOLDER="$PROJECT_ROOT/src/main/resources"
LOCAL_LIB_FOLDER="$PROJECT_ROOT/lib"
BUILD_FOLDER="$PROJECT_ROOT/target/classes"
OUTPUT_JAR="$PROJECT_ROOT/target/ai4mbse-plugin.jar"
SOURCES_LIST="$PROJECT_ROOT/target/sources.txt"

# 4. AUFRÄUMEN
echo "--- Räume auf ---"
rm -rf "$BUILD_FOLDER"
mkdir -p "$BUILD_FOLDER"
mkdir -p "$PROJECT_ROOT/target"

# 5. KOMPILIEREN
echo "--- Kompiliere ---"

# Classpath (Doppelpunkt statt Semikolon auf Mac!)
CLASSPATH="$LIB_DIR/*:$LOCAL_LIB_FOLDER/*"

# Java Dateien finden
find "$SRC_FOLDER" -name "*.java" > "$SOURCES_LIST"

if [ ! -s "$SOURCES_LIST" ]; then
    echo -e "${RED}Keine Java-Dateien gefunden!${NC}"
    exit 1
fi

# Compiler starten
$JAVAC_TOOL -encoding UTF-8 -cp "$CLASSPATH" -d "$BUILD_FOLDER" @"$SOURCES_LIST"

if [ $? -ne 0 ]; then
    echo -e "${RED}Kompilierung fehlgeschlagen!${NC}"
    exit 1
fi

# 6. RESSOURCEN KOPIEREN
if [ -d "$RESOURCE_FOLDER" ]; then
    cp -R "$RESOURCE_FOLDER/"* "$BUILD_FOLDER/"
fi

# 7. JAR PACKEN
echo "--- Erstelle JAR ---"
$JAR_TOOL -cf "$OUTPUT_JAR" -C "$BUILD_FOLDER" .

if [ $? -ne 0 ]; then
    echo -e "${RED}JAR Erstellung fehlgeschlagen!${NC}"
    exit 1
fi
echo -e "${GREEN}Build erfolgreich: $OUTPUT_JAR${NC}"

# 8. INSTALLIEREN
echo "--- Installiere Plugin ---"
echo "Ziel: $PLUGIN_INSTALL_DIR"

# Da wir in /Applications schreiben, brauchen wir oft 'sudo'.
# Wir versuchen es erst normal, wenn das fehlschlägt, fragen wir nach sudo.

if [ ! -w "$PLUGIN_BASE_DIR" ]; then
    echo -e "${CYAN}HINWEIS: Der Zielordner ist schreibgeschützt. Ich benötige dein Passwort (sudo).${NC}"
    USE_SUDO="sudo"
else
    USE_SUDO=""
fi

# Ordner erstellen
$USE_SUDO mkdir -p "$PLUGIN_INSTALL_DIR"

# JAR kopieren
$USE_SUDO cp -f "$OUTPUT_JAR" "$PLUGIN_INSTALL_DIR"

# plugin.xml kopieren (falls vorhanden)
if [ -f "$RESOURCE_FOLDER/plugin.xml" ]; then
    $USE_SUDO cp -f "$RESOURCE_FOLDER/plugin.xml" "$PLUGIN_INSTALL_DIR"
fi

echo -e "${GREEN}Installation fertig! Bitte MagicDraw/Cameo neu starten.${NC}"