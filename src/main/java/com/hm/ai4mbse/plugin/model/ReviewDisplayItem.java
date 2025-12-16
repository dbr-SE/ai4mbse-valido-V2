package com.hm.ai4mbse.plugin.model;

/**
 * View-Model.
 * Dieses Objekt wird von der Visualisierung erzeugt und vom UI nur angezeigt.
 * Es enthält bereits formatierte Texte und Logik für die Anzeige.
 */
public class ReviewDisplayItem {
    private String elementColumn;    // Was steht in Spalte 1
    private String problemColumn;    // Was steht in Spalte 2
    private String confidenceColumn; // Was steht in Spalte 3 (z.B. "95% (Hoch)")
    private String explanationText;  // Der Text für den (?) Button

    public ReviewDisplayItem(String elementColumn, String problemColumn, String confidenceColumn, String explanationText) {
        this.elementColumn = elementColumn;
        this.problemColumn = problemColumn;
        this.confidenceColumn = confidenceColumn;
        this.explanationText = explanationText;
    }

    public String getElementColumn() { return elementColumn; }
    public String getProblemColumn() { return problemColumn; }
    public String getConfidenceColumn() { return confidenceColumn; }
    public String getExplanationText() { return explanationText; }
}