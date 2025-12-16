package com.hm.ai4mbse.plugin.catiamsosa;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;

import java.io.File;

/**
 * Helferklasse für den Export des Modells.
 * Unterstützt sowohl ZIP-Snapshots (Backup) als auch XML (für KI).
 */
public class ModelExportHelper {

    /**
     * Erstellt einen Snapshot als komprimierte .mdzip Datei.
     * @return Das File-Objekt oder null bei Fehler.
     */
    public static File createSnapshot(Project project) {
        return exportProject(project, ".mdzip");
    }

    /**
     * Erstellt einen Snapshot als unkomprimierte .xml Datei (Marius' Ansatz).
     * @return Das File-Objekt oder null bei Fehler.
     */
    public static File createXmlSnapshot(Project project) {
        // Die Endung .xml sorgt dafür, dass MagicDraw es als Text speichert.
        return exportProject(project, ".xml");
    }

    /**
     * Interne Methode, die die eigentliche Arbeit macht.
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

            // 2. Dateinamen generieren (Sonderzeichen entfernen)
            String safeName = project.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
            File targetFile = new File(tempDir, safeName + "_snapshot" + extension);

            // Alte Datei löschen
            if (targetFile.exists()) targetFile.delete();

            logToGUI("Export gestartet: " + targetFile.getName());

            // 3. Deskriptor erstellen (Verbindet Projekt mit Zieldatei)
            ProjectDescriptor descriptor = ProjectDescriptorsFactory.createLocalProjectDescriptor(project, targetFile);

            // 4. Speichern (Silent = true unterdrückt Popups)
            ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
            projectsManager.saveProject(descriptor, true);

            logToGUI("Export fertig: " + targetFile.getAbsolutePath());
            return targetFile;

        } catch (Exception e) {
            logToGUI("Exportfehler: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void logToGUI(String msg) {
        GUILog log = Application.getInstance().getGUILog();
        if (log != null) {
            log.log("[AI4MBSE Export] " + msg);
        } else {
            System.out.println("[AI4MBSE Export] " + msg);
        }
    }
}

