package com.hm.ai4mbse.plugin.modules.config;

import com.hm.ai4mbse.plugin.interfaces.IConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Eine einfache, dateibasierte Implementierung des IConfig-Interfaces.
 * (Aktuell wird die Datei-Interaktion nur simuliert)
 */
public class PluginConfig implements IConfig {

    private final Map<String, String> properties = new HashMap<>();

    @Override
    public void load() {
        // In Zukunft: Lade Properties aus einer Datei (z.B. plugin.properties)
        System.out.println("[PluginConfig] Lade Konfiguration... (Simulation)");
        // Beispiel-Properties für die Simulation
        properties.put("apiKey", "default-api-key");
        properties.put("reviewMode", "strict");
    }

    @Override
    public void save() {
        // In Zukunft: Speichere Properties in eine Datei
        System.out.println("[PluginConfig] Speichere Konfiguration... (Simulation)");
    }

    @Override
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
}
