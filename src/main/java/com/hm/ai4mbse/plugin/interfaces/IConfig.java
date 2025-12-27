package com.hm.ai4mbse.plugin.interfaces;
import java.util.Optional;

public interface IConfig {
    void load();
    void save();
    Optional<String> getProperty(String key);
    void setProperty(String key, String value);
}