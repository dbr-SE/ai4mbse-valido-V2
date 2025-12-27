package com.hm.ai4mbse.plugin.catiamsosa;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helferklasse für den Export des Modells.
 * UPDATE V6: Erkennt nun auch 'com.nomagic.magicdraw.uml_model.model'.
 */
public class ModelExportHelper {

    public static File createXmlSnapshot(Project project) {
        File rawExport = exportProjectByCopying(project);
        if (rawExport == null || !rawExport.exists()) return null;

        try {
            logToGUI("Starte KI-Optimierung (Smart Cleaning)...");
            long startSize = rawExport.length();

            // Cleaning starten
            File cleanFile = cleanXmlForAI(rawExport);

            long endSize = cleanFile.length();
            logToGUI("Optimierung fertig. Größe: " + (startSize/1024) + "KB -> " + (endSize/1024) + "KB");

            rawExport.delete(); // Rohdatei löschen
            return cleanFile;
        } catch (Exception e) {
            logToGUI("Warnung: Cleaning fehlgeschlagen. Nutze Rohdatei.");
            e.printStackTrace();
            return rawExport;
        }
    }

    private static File exportProjectByCopying(Project project) {
        if (project == null) {
            logToGUI("FEHLER: Kein aktives Projekt.");
            return null;
        }

        ProjectsManager pm = Application.getInstance().getProjectsManager();

        if (project.isDirty()) {
            logToGUI("Speichere aktuelles Projekt...");
            ProjectDescriptor descriptor = ProjectDescriptorsFactory.getDescriptorForProject(project);
            pm.saveProject(descriptor, true);
        }

        String projectPath = project.getFileName();
        if (projectPath == null) {
            logToGUI("FEHLER: Projektpfad ist null.");
            return null;
        }

        File originalFile = new File(projectPath);
        if (!originalFile.exists()) {
            logToGUI("FEHLER: Datei nicht gefunden: " + projectPath);
            return null;
        }

        try {
            File targetDir = getExportDirectory();
            String safeName = project.getName().replaceAll("[^a-zA-Z0-9.-]", "_");

            String fName = originalFile.getName().toLowerCase();
            if (fName.endsWith(".mdzip") || fName.endsWith(".zip") || fName.endsWith(".jar")) {
                // Hier gehen wir ins Archiv
                return extractModelFromZip(originalFile, targetDir, safeName + "_raw.xml");
            } else {
                // Direkte XML/MDXML Datei
                File targetFile = new File(targetDir, safeName + "_raw.xml");
                Files.copy(originalFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return targetFile;
            }

        } catch (Exception e) {
            logToGUI("Exportfehler: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static File extractModelFromZip(File zipFile, File targetDir, String targetName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                String name = zipEntry.getName();
                String lower = name.toLowerCase();

                if (zipEntry.isDirectory()) continue;

                // --- HIER IST DIE ANPASSUNG ---
                // 1. Der interne MagicDraw Name (Volltreffer aus deinem Log)
                if (name.equals("com.nomagic.magicdraw.uml_model.model")) {
                    logToGUI("Modell gefunden (Internal Format): " + name);
                    return writeZipEntryToFile(zis, targetDir, targetName);
                }

                // 2. Standard XML Endungen (Fallback)
                if (lower.endsWith(".mdxml") || lower.endsWith(".xml") || lower.endsWith(".xmi") || lower.endsWith(".uml")) {
                    logToGUI("Modell gefunden (XML Format): " + name);
                    return writeZipEntryToFile(zis, targetDir, targetName);
                }
            }
        }
        throw new IOException("Keine Modelldatei im Archiv gefunden. (Geprüft auf: com.nomagic.magicdraw.uml_model.model, .mdxml, .xml, .xmi)");
    }

    private static File writeZipEntryToFile(InputStream zis, File targetDir, String targetName) throws IOException {
        File targetFile = new File(targetDir, targetName);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
        return targetFile;
    }

    // --- CLEANING ---
    private static File cleanXmlForAI(File inputFile) throws IOException {
        File outputFile = new File(inputFile.getParent(), inputFile.getName().replace("_raw.xml", "_snapshot.xml"));
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            String line;
            boolean insideSkipBlock = false;
            String currentSkipTag = "";
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // XML Header oder Binärdaten filtern
                if (!insideSkipBlock) {
                    if (trimmed.startsWith("<diagram") || trimmed.startsWith("<Diagram")) {
                        insideSkipBlock = true; currentSkipTag = "diagram";
                    } else if (trimmed.startsWith("<xmi:Extension")) {
                        insideSkipBlock = true; currentSkipTag = "xmi:Extension";
                    } else if (trimmed.startsWith("<binary")) {
                        insideSkipBlock = true; currentSkipTag = "binary";
                    }
                    if (insideSkipBlock) continue;
                }
                if (insideSkipBlock) {
                    if (trimmed.contains("</" + currentSkipTag + ">") || trimmed.endsWith("/>") || (currentSkipTag.equals("diagram") && trimmed.contains("</Diagram>"))) {
                        insideSkipBlock = false; currentSkipTag = "";
                    }
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
        }
        return outputFile;
    }

    // --- HELPER ---
    private static void logToGUI(String msg) {
        GUILog log = Application.getInstance().getGUILog();
        if (log != null) log.log("[AI4MBSE] " + msg);
        else System.out.println("[AI4MBSE] " + msg);
    }

    private static File getExportDirectory() {
        try {
            File configFile = new File(System.getProperty("user.home"), ".ai4mbse/config.properties");
            if (configFile.exists()) {
                List<String> lines = Files.readAllLines(configFile.toPath());
                for (String line : lines) {
                    if (line.startsWith("export_path=")) {
                        String path = line.substring("export_path=".length()).trim();
                        File customDir = new File(path);
                        if (!customDir.exists()) customDir.mkdirs();
                        if (customDir.exists() && customDir.canWrite()) return customDir;
                    }
                }
            }
        } catch (Exception e) { /* Ignore */ }
        File fallback = new File(System.getProperty("java.io.tmpdir"), "ai4mbse_export");
        if (!fallback.exists()) fallback.mkdirs();
        return fallback;
    }
}