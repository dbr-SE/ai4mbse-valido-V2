#!/bin/bash

# =============================================================================
# BUILD SKRIPT FÜR AI4MBSE PLUGIN (MACOS) - OPTIMIERT
# =============================================================================

# Farben
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# 1. PFADE
PROGRAM_DIR="/Applications/Magic Systems of Systems Architect"
LIB_DIR="$PROGRAM_DIR/lib"
PLUGIN_BASE_DIR="/Applications/Magic Systems of Systems Architect/plugins"
PLUGIN_INSTALL_DIR="$PLUGIN_BASE_DIR/AI4MBSE_valido"

JAR_TOOL="jar"
JAVAC_TOOL="javac"

# 2. CHECKS
echo "--- Starte Checks ---"
if [ ! -f "$LIB_DIR/md.jar" ]; then
    echo -e "${RED}FEHLER: 'md.jar' nicht gefunden in: $LIB_DIR${NC}"
    exit 1
else
    echo -e "${GREEN}Bibliotheken gefunden.${NC}"
fi

# 3. PROJEKT-PFADE
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
# Classpath inklusive MagicDraw Libs UND lokaler Libs
CLASSPATH="$LIB_DIR/*:$LIB_DIR/bundles/*:$LIB_DIR/plugins/*:$LOCAL_LIB_FOLDER/*"

find "$SRC_FOLDER" -name "*.java" > "$SOURCES_LIST"

if [ ! -s "$SOURCES_LIST" ]; then
    echo -e "${RED}Keine Java-Dateien gefunden!${NC}"
    exit 1
fi

$JAVAC_TOOL -encoding UTF-8 -cp "$CLASSPATH" -d "$BUILD_FOLDER" @"$SOURCES_LIST"

if [ $? -ne 0 ]; then
    echo -e "${RED}Kompilierung fehlgeschlagen!${NC}"
    exit 1
fi

# 6. RESSOURCEN KOPIEREN (HIER WAR DAS PROBLEM EVENTUELL)
echo "--- Kopiere Ressourcen ---"
if [ -d "$RESOURCE_FOLDER" ]; then
    # Kopiere alles rekursiv
    cp -R "$RESOURCE_FOLDER/"* "$BUILD_FOLDER/"

    # CHECK: Wurde die DB kopiert?
    if [ -f "$BUILD_FOLDER/std_rules_db.json" ]; then
        echo -e "${GREEN}OK: std_rules_db.json erfolgreich kopiert.${NC}"
    else
        echo -e "${RED}WARNUNG: std_rules_db.json wurde NICHT kopiert!${NC}"
    fi
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

# Sudo Logik
if [ ! -w "$PLUGIN_BASE_DIR" ]; then
    echo -e "${CYAN}Passwort für Installation erforderlich (sudo)...${NC}"
    USE_SUDO="sudo"
else
    USE_SUDO=""
fi

$USE_SUDO mkdir -p "$PLUGIN_INSTALL_DIR"
$USE_SUDO cp -f "$OUTPUT_JAR" "$PLUGIN_INSTALL_DIR"

# WICHTIG: Auch die JSONs müssen ggf. im Plugin-Ordner liegen,
# falls sie nicht im JAR gefunden werden (Sicherheitsnetz)
if [ -f "$RESOURCE_FOLDER/std_rules_db.json" ]; then
   $USE_SUDO cp "$RESOURCE_FOLDER/std_rules_db.json" "$PLUGIN_INSTALL_DIR/"
fi
if [ -f "$RESOURCE_FOLDER/rules_db.json" ]; then
   $USE_SUDO cp "$RESOURCE_FOLDER/rules_db.json" "$PLUGIN_INSTALL_DIR/"
fi

# plugin.xml
if [ -f "$RESOURCE_FOLDER/plugin.xml" ]; then
    $USE_SUDO cp -f "$RESOURCE_FOLDER/plugin.xml" "$PLUGIN_INSTALL_DIR"
fi

echo -e "${GREEN}Installation fertig! Bitte MagicDraw neu starten.${NC}"