package com.hm.ai4mbse.plugin.modules.visualization;

import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.ReviewIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizationService {

    /**
     * Liest den Text-Report (Format aus review_result_demo-001.txt) und
     * wandelt ihn in interne ReviewIssue-Objekte um.
     */
    public List<ReviewIssue> parseTextReportToIssues(String reportText) {
        List<ReviewIssue> issues = new ArrayList<>();

        // REGEX für das Format:
        // Severity: FEHLER; Element-ID: ...; Element-Name: ...; Element-Typ: ...; Beschreibung: ...; Empfehlung: ...
        // Wir nutzen (?s) für Dotall mode, falls Zeilenumbrüche vorkommen, und [^;]+ um bis zum nächsten Semikolon zu lesen.
        String regex = "Severity:\\s*([^;]+);\\s*Element-ID:\\s*([^;]+);\\s*Element-Name:\\s*([^;]+);\\s*Element-Typ:\\s*([^;]+);\\s*Beschreibung:\\s*([^;]+);\\s*Empfehlung:\\s*(.*)";

        Pattern pattern = Pattern.compile(regex);
        // Wir splitten den Text in Zeilen/Absätze, um jeden Befund einzeln zu matchen
        String[] lines = reportText.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().startsWith("Severity:")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String severity = matcher.group(1).trim();
                    String id = matcher.group(2).trim();
                    String name = matcher.group(3).trim();
                    String type = matcher.group(4).trim();
                    String desc = matcher.group(5).trim();
                    String recommendation = matcher.group(6).trim();

                    // Wir bauen den Typstring für die Anzeige: "FEHLER (Block)"
                    String displayType = severity + " (" + type + ")";

                    // Konfidenz simulieren (da nicht im Text enthalten)
                    int confidence = severity.equalsIgnoreCase("FEHLER") ? 99 :
                            severity.equalsIgnoreCase("WARNUNG") ? 75 : 100;

                    // Text für die Spalte "Problem / Vorschlag"
                    String combinedText = desc + " -> " + recommendation;

                    issues.add(new ReviewIssue(name, displayType, combinedText, confidence));
                }
            }
        }
        return issues;
    }

    /**
     * Wandelt die Issues in das Format für die UI-Tabelle um.
     */
    public List<ReviewDisplayItem> prepareDisplayData(List<ReviewIssue> rawIssues) {
        List<ReviewDisplayItem> displayItems = new ArrayList<>();

        for (ReviewIssue issue : rawIssues) {
            String displayConfidence = issue.getConfidence() + "%";

            // HTML für das Hilfe-Popup (?)
            String explanation = "<html><body style='width: 300px;'>" +
                    "<b>Problem:</b> " + issue.getIssueType() + "<br><br>" +
                    "<b>Detail:</b> " + issue.getSuggestion() +
                    "</body></html>";

            displayItems.add(new ReviewDisplayItem(
                    issue.getElementName(),
                    issue.getIssueType(), // Hier steht jetzt z.B. "FEHLER (Fehlende Zahl)"
                    displayConfidence,
                    explanation
            ));
        }
        return displayItems;
    }
}