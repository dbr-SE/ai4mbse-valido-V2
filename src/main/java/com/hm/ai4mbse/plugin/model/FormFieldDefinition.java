package com.hm.ai4mbse.plugin.model;

/**
 * Beschreibt ein Eingabefeld für das dynamische UI.
 * Ermöglicht dem Backend zu steuern, welche Fragen im UI gestellt werden.
 */
public class FormFieldDefinition {
    private String id;          // Interne ID zur Identifikation (z.B. "level")
    private String label;       // Angezeigter Text (z.B. "System Level:")
    private FieldType type;     // Art des Feldes
    private String[] options;   // Nur für Dropdowns relevant

    public enum FieldType {
        TEXT_FIELD,
        DROPDOWN
    }

    // Konstruktor für Textfelder
    public FormFieldDefinition(String id, String label) {
        this.id = id;
        this.label = label;
        this.type = FieldType.TEXT_FIELD;
        this.options = new String[]{};
    }

    // Konstruktor für Dropdowns
    public FormFieldDefinition(String id, String label, String[] options) {
        this.id = id;
        this.label = label;
        this.type = FieldType.DROPDOWN;
        this.options = options;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public FieldType getType() { return type; }
    public String[] getOptions() { return options; }
}