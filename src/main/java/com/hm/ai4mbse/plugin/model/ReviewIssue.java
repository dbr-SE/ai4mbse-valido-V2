package com.hm.ai4mbse.plugin.model;

/**
 * Model-Klasse für ein identifiziertes Problem im Review.
 * Repräsentiert eine Zeile im Ergebnisbericht.
 */
public class ReviewIssue {
    private String elementName;
    private String issueType; // z.B. "Muda", "Traceability Gap"
    private String suggestion;
    private int confidence;   // 0-100%

    public ReviewIssue(String elementName, String issueType, String suggestion, int confidence) {
        this.elementName = elementName;
        this.issueType = issueType;
        this.suggestion = suggestion;
        this.confidence = confidence;
    }

    // Getter methoden werden bereitgestellt.
    public String getElementName() { return elementName; }
    public String getIssueType() { return issueType; }
    public String getSuggestion() { return suggestion; }
    public int getConfidence() { return confidence; }
}