package com.hm.ai4mbse.plugin.modules.database;

import com.hm.ai4mbse.plugin.interfaces.DatabaseService;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Dummy-Service für Tests ohne echte Datenbank.
 */
public class MockDatabaseService implements DatabaseService {

    @Override
    public List<RuleDefinition> loadRules() {
        // Dummy-Liste zurückgeben
        List<RuleDefinition> list = new ArrayList<>();
        RuleDefinition r = new RuleDefinition();
        r.put("regeltitel", "Mock Regel 1");
        r.put("ziel", "Test");
        list.add(r);
        return list;
    }

    @Override
    public List<RuleDefinition> loadStandardRules() {
        // Leere Liste oder Dummys für Standard-Regeln
        return new ArrayList<>();
    }

    @Override
    public void saveRule(RuleDefinition rule) {
        System.out.println("[MockDB] Speichere Regel: " + rule.get("regeltitel"));
    }

    @Override
    public void deleteRule(RuleDefinition rule) {
        System.out.println("[MockDB] Lösche Regel: " + rule.get("regeltitel"));
    }
}