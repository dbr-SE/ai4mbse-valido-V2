package com.hm.ai4mbse.plugin.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model-Klasse für eine Regel.
 * Flexibel (Map) für Michis KI-Felder, aber kompatibel zum alten Code.
 */
public class RuleDefinition {
    // Flexible Speicherung aller Felder
    private Map<String, String> data = new HashMap<>();

    // 1. Neuer Leerer Konstruktor (für das dynamische UI & Michi)
    public RuleDefinition() {}

    // 2. RETTUNGS-ANKER: Der alte Konstruktor (für Kompatibilität)
    // Wenn alter Code "new RuleDefinition(level, element, criteria)" aufruft,
    // fangen wir das hier ab und speichern es in die Map.
    public RuleDefinition(String sysLevel, String elementType, String criteria) {
        put("sys_level", sysLevel);
        put("element_type", elementType);
        put("criteria", criteria);

        // Wir setzen auch Michis Felder auf sinnvolle Defaults, damit nichts crasht
        put("regeltitel", "Legacy Regel (" + elementType + ")");
        put("ziel", criteria);
    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.getOrDefault(key, "");
    }

    public Map<String, String> getData() {
        return data;
    }

    // --- Wrapper für Kompatibilität mit altem Code ---
    public String getSystemLevel() { return get("sys_level"); }
    public String getElementType() { return get("element_type"); }
    public String getCheckCriteria() { return get("criteria"); }

    @Override
    public String toString() {
        // Zeige Titel wenn vorhanden, sonst Fallback
        return data.containsKey("regeltitel") && !data.get("regeltitel").isEmpty()
                ? data.get("regeltitel")
                : String.format("[%s] %s", get("sys_level"), get("criteria"));
    }
}