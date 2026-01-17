package com.hm.ai4mbse.plugin.catiamsosa;

import com.hm.ai4mbse.plugin.core.Orchestrator;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.ActionsCategory;

import java.awt.event.ActionEvent;

/**
 * Lifecycle des Plugins (Optimiert für Feature 1):
 * 1. Initialisiert den Orchestrator.
 * 2. Registriert NUR den Button im Tools-Menü.
 * 3. KEIN automatischer Export mehr beim Start (passiert jetzt manuell im UI).
 */
public class PluginLifecycle extends Plugin {

    private Orchestrator orchestrator;

    @Override
    public void init() {
        Application.getInstance().getGUILog().log("[AI4MBSE] Initialisiere Plugin (Manual Mode)...");

        // Orchestrator bereitstellen (wartet jetzt auf User-Input)
        orchestrator = new Orchestrator();

        // --- UI BUTTON (Tools Menü) ---
        ActionsConfiguratorsManager.getInstance()
                .addMainMenuConfigurator(manager -> addToolsMenuAction(manager));

        System.out.println("[AI4MBSE] Plugin initialized. Waiting for user command.");
    }

    /**
     * Erstellt den Menü-Eintrag unter "Tools" (Extras).
     */
    private void addToolsMenuAction(ActionsManager manager) {
        // 1. Die Action erstellen
        NMAction openUiAction = new NMAction("AI4MBSE_OPEN_UI", "VALIDO öffnen", null) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Öffnet das Hauptfenster
                orchestrator.showMainWindow();
            }
        };

        // 2. Kategorie "Tools" finden oder erstellen
        ActionsCategory toolsCategory = manager.getCategory("TOOLS");

        if (toolsCategory != null) {
            toolsCategory.addAction(openUiAction);
        } else {
            ActionsCategory myCategory = new ActionsCategory("AI4MBSE_MENU", "AI4MBSE");
            myCategory.addAction(openUiAction);
            manager.addCategory(myCategory);
        }
    }

    @Override
    public boolean close() { return true; }

    @Override
    public boolean isSupported() { return true; }
}