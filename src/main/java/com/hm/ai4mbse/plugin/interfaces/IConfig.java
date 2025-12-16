package com.hm.ai4mbse.plugin.interfaces;

import java.util.Optional;

/**
 * Defines the contract for managing plugin configuration.
 * This interface allows for loading, saving, and accessing
 * configuration properties in a standardized way.
 */
public interface IConfig {

    /**
     * Loads the configuration from a persistent source.
     */
    void load();

    /**
     * Saves the current configuration to a persistent source.
     */
    void save();

    /**
     * Retrieves a configuration property by its key.
     *
     * @param key The key of the property to retrieve.
     * @return An Optional containing the property value, or an empty Optional if the key is not found.
     */
    Optional<String> getProperty(String key);

    /**
     * Sets or updates a configuration property.
     *
     * @param key   The key of the property to set.
     * @param value The value of the property.
     */
    void setProperty(String key, String value);
}
