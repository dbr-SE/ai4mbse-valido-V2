package com.hm.ai4mbse.plugin.modules.database;

import com.hm.ai4mbse.plugin.interfaces.DatabaseService;
import com.hm.ai4mbse.plugin.model.RuleDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulierte Datenbank-Implementierung.
 * Daten werden temporär in einer Liste gehalten, um die Datenbank zu simulieren.
 */
public class MockDatabaseService implements DatabaseService {

    private List<RuleDefinition> internalStorage;

    public MockDatabaseService() {
        this.internalStorage = new ArrayList<>();
        // Beispielwerte werden für Testzwecke generiert.
        this.internalStorage.add(new RuleDefinition("System Context", "Block", "Keine Zyklen erlaubt"));
        this.internalStorage.add(new RuleDefinition("Logical", "Interface", "Ports müssen typisiert sein"));
    }

    @Override
    public List<RuleDefinition> loadAllRules() {
        // Eine Kopie der Liste wird zurückgegeben.
        return new ArrayList<>(internalStorage);
    }

    @Override
    public void saveRule(RuleDefinition rule) {
        // Die Regel wird zur Liste hinzugefügt.
        this.internalStorage.add(rule);
        System.out.println("LOG [Database]: Rule saved: " + rule.toString());
    }
}