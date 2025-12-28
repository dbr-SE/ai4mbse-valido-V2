package com.hm.ai4mbse.plugin.catiamsosa;

import com.hm.ai4mbse.plugin.core.Orchestrator;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.ActionsCategory;

import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Lifecycle des Plugins:
 * 1. Initialisiert den Orchestrator.
 * 2. Startet automatischen XML-Export im Hintergrund.
 * 3. Registriert den "Open AI Assistant" Button im Tools-Menü.
 */
public class PluginLifecycle extends Plugin {

    private Orchestrator orchestrator;

    @Override // WICHTIG: Überschreibt Methode aus 'Plugin'
    public void init() {
        Application.getInstance().getGUILog().log("[AI4MBSE] Initialisiere Plugin...");
        orchestrator = new Orchestrator();

        // --- A) AUTO-EXPORT (Hintergrund) ---

        // 1. Falls beim Start (Reload) schon ein Projekt da ist
        Project currentProject = Application.getInstance().getProject();
        if (currentProject != null) {
            new Thread(() -> performAutoExport(currentProject)).start();
        }

        // 2. Listener für neu geöffnete Projekte
        Application.getInstance().addProjectEventListener(new ProjectEventListenerAdapter() {
            @Override // WICHTIG: Überschreibt Methode aus Adapter
            public void projectOpened(Project project) {
                new Thread(() -> performAutoExport(project)).start();
            }
        });

        // --- B) UI BUTTON (Tools Menü) ---
        ActionsConfiguratorsManager.getInstance()
                .addMainMenuConfigurator(manager -> addToolsMenuAction(manager));

        System.out.println("[AI4MBSE] Plugin fully initialized.");
    }

    /**
     * Führt den Export durch und übergibt das Ergebnis an den Orchestrator.
     */
    private void performAutoExport(Project project) {
        if (project == null) return;

        // XML Export nutzen (Dein neuer Helper)
        File exportedFile = ModelExportHelper.createXmlSnapshot(project);

        if (exportedFile != null) {
            // Dem Orchestrator die Datei geben
            orchestrator.startWithFile(exportedFile);
            Application.getInstance().getGUILog().log("[AI4MBSE] Analyse bereit. Datei: " + exportedFile.getName());
        }
    }

    /**
     * Erstellt den Menü-Eintrag unter "Tools" (Extras).
     */
    private void addToolsMenuAction(ActionsManager manager) {
        // 1. Die Action erstellen
        NMAction openUiAction = new NMAction("AI4MBSE_OPEN_UI", "VALIDO öffnen", null) {
            @Override // WICHTIG: Überschreibt Methode aus NMAction
            public void actionPerformed(ActionEvent e) {
                // Hier öffnen wir nur noch das UI!
                orchestrator.showMainWindow();
            }
        };

        // 2. Kategorie "Tools" finden
        ActionsCategory toolsCategory = manager.getCategory("TOOLS");

        if (toolsCategory != null) {
            // In bestehendes Tools-Menü einfügen
            toolsCategory.addAction(openUiAction);
        } else {
            // Fallback: Eigenes Menü, falls Tools nicht gefunden wird
            ActionsCategory myCategory = new ActionsCategory("AI4MBSE_MENU", "AI4MBSE");
            myCategory.addAction(openUiAction);
            manager.addCategory(myCategory);
        }
    }

    @Override // WICHTIG
    public boolean close() { return true; }

    @Override // WICHTIG
    public boolean isSupported() { return true; }
}