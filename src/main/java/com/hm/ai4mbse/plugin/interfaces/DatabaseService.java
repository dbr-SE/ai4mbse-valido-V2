package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.RuleDefinition;
import java.util.List;

/**
 * Interface für das Datenbank-Subsystem.
 * Es werden Methoden zur Speicherung, zum Laden und Löschen von Regeln definiert.
 */
public interface DatabaseService {

    /**
     * Alle benutzerdefinierten Regeln (Custom Rules) werden aus der Datenbank geladen.
     * @return Eine Liste der RuleDefinition Objekte.
     */
    List<RuleDefinition> loadRules();

    /**
     * Alle Standard-Regeln (Standard User Stories) werden aus der Read-Only-Datenbank geladen.
     * @return Eine Liste der RuleDefinition Objekte.
     */
    List<RuleDefinition> loadStandardRules();

    /**
     * Eine neue Regel wird in der Datenquelle gespeichert oder aktualisiert.
     * @param rule Die zu speichernde Regel.
     */
    void saveRule(RuleDefinition rule);

    /**
     * Eine vorhandene Regel wird aus der Datenquelle entfernt.
     * @param rule Die zu löschende Regel.
     */
    void deleteRule(RuleDefinition rule);
}