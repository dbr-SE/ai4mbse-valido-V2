package com.hm.ai4mbse.plugin.catiamsosa;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Helferklasse für den Export des Modells.
 * Kombiniert den stabilen Standard-Export mit Smart-Cleaning für KI-Optimierung.
 */
public class ModelExportHelper {

    /**
     * Erstellt einen Snapshot als unkomprimierte .xml Datei und bereinigt sie für die KI.
     * @return Das File-Objekt oder null bei Fehler.
     */
    public static File createXmlSnapshot(Project project) {
        // 1. Der stabile Export (Dein Original-Code)
        File rawExport = exportProject(project, ".xml");

        if (rawExport == null || !rawExport.exists()) {
            return null;
        }

        // 2. Smart Cleaning dazwischenschalten
        try {
            logToGUI("Starte KI-Optimierung (Smart Cleaning)...");
            long startSize = rawExport.length();

            File cleanFile = cleanXmlForAI(rawExport);

            long endSize = cleanFile.length();
            logToGUI("Optimierung fertig. Größe: " + (startSize/1024) + "KB -> " + (endSize/1024) + "KB");

            // Optional: Die riesige Rohdatei löschen, wir brauchen nur die saubere
            rawExport.delete();

            return cleanFile;

        } catch (Exception e) {
            logToGUI("Warnung: Cleaning fehlgeschlagen (" + e.getMessage() + "). Nutze Rohdatei.");
            e.printStackTrace();
            return rawExport; // Fallback: Wenn Putzen scheitert, nehmen wir das Original
        }
    }

    /**
     * Interne Methode, die die eigentliche Arbeit macht (DEIN ORIGINAL CODE).
     */
    private static File exportProject(Project project, String extension) {
        if (project == null) {
            logToGUI("FEHLER: Kein aktives Projekt gefunden.");
            return null;
        }

        try {
            // 1. Temp-Ordner
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "ai4mbse_export");
            if (!tempDir.exists()) tempDir.mkdirs();

            // 2. Dateinamen generieren
            String safeName = project.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            File targetFile = new File(tempDir, safeName + "_snapshot" + extension);

            // Alte Datei löschen
            if (targetFile.exists()) targetFile.delete();

            logToGUI("Export gestartet: " + targetFile.getName());

            // 3. Deskriptor erstellen (HIER LAG DER SCHLÜSSEL!)
            ProjectDescriptor descriptor = ProjectDescriptorsFactory.createLocalProjectDescriptor(project, targetFile);

            // 4. Speichern
            ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
            projectsManager.saveProject(descriptor, true);

            logToGUI("Export gespeichert in: " + targetFile.getAbsolutePath());
            return targetFile;

        } catch (Exception e) {
            logToGUI("Exportfehler: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Entfernt Diagramm-Daten und Tool-Extensions aus der XML.
     * (Der Code, den wir vorhin geschrieben haben)
     */
    private static File cleanXmlForAI(File inputFile) throws IOException {
        // Neue Datei im gleichen Ordner erstellen
        File outputFile = new File(inputFile.getParent(), "clean_" + inputFile.getName());

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {

            String line;
            boolean insideSkipBlock = false;
            String currentSkipTag = "";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                // Start eines Blocks erkennen, den wir löschen wollen
                if (!insideSkipBlock) {
                    if (trimmed.startsWith("<diagram")) {
                        insideSkipBlock = true;
                        currentSkipTag = "diagram";
                    } else if (trimmed.startsWith("<xmi:Extension")) {
                        insideSkipBlock = true;
                        currentSkipTag = "xmi:Extension";
                    } else if (trimmed.startsWith("<binary")) {
                        insideSkipBlock = true;
                        currentSkipTag = "binary";
                    }

                    if (insideSkipBlock) continue; // Start-Tag überspringen
                }

                // Ende des Blocks suchen
                if (insideSkipBlock) {
                    if (trimmed.contains("</" + currentSkipTag + ">") || trimmed.endsWith("/>")) {
                        insideSkipBlock = false;
                        currentSkipTag = "";
                    }
                    continue; // Inhalt überspringen
                }

                // Zeile behalten
                writer.write(line);
                writer.newLine();
            }
        }
        return outputFile;
    }

    private static void logToGUI(String msg) {
        GUILog log = Application.getInstance().getGUILog();
        if (log != null) {
            log.log("[AI4MBSE] " + msg);
        } else {
            System.out.println("[AI4MBSE] " + msg);
        }
    }
}