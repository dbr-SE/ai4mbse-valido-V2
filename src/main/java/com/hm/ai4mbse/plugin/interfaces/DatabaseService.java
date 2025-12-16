package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.RuleDefinition;
import java.util.List;

/**
 * Interface für das Datenbank-Subsystem.
 * [cite_start]Methoden zur Speicherung und zum Laden werden hier definiert[cite: 23].
 */
public interface DatabaseService {
    /**
     * Alle Regeln werden aus der Datenquelle geladen.
     * @return Eine Liste der RuleDefinition Objekte.
     */
    List<RuleDefinition> loadAllRules();

    /**
     * Eine neue Regel wird in der Datenquelle gespeichert.
     * @param rule Die zu speichernde Regel.
     */
    void saveRule(RuleDefinition rule);
}