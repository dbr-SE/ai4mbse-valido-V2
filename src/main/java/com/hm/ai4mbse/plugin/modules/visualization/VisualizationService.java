package com.hm.ai4mbse.plugin.modules.visualization;

import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.ReviewIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisualizationService {

    // Interner Container für die Analyse
    public static class AnalysisResult {
        public enum Status { SUCCESS_NO_ISSUES, ISSUES_FOUND, PARSING_ERROR }
        private final Status status;
        private final List<ReviewIssue> issues;

        public AnalysisResult(Status status, List<ReviewIssue> issues) {
            this.status = status;
            this.issues = issues;
        }
        public Status getStatus() { return status; }
        public List<ReviewIssue> getIssues() { return issues; }
    }

    /**
     * Analysiert den Text und entscheidet über den Status.
     */
    public AnalysisResult analyzeReport(String reportText) {
        List<ReviewIssue> issues = new ArrayList<>();

        if (reportText == null || reportText.isBlank()) {
            return new AnalysisResult(AnalysisResult.Status.PARSING_ERROR, issues);
        }

        // 1. Check auf Erfolg (Case-Insensitive)
        if (reportText.toLowerCase().contains("keine verstöße gefunden")) {
            return new AnalysisResult(AnalysisResult.Status.SUCCESS_NO_ISSUES, issues);
        }

        // 2. Parsen nach Issues
        String regex = "Severity:\\s*([^;]+);\\s*Element-ID:\\s*([^;]+);\\s*Element-Name:\\s*([^;]+);\\s*Element-Typ:\\s*([^;]+);\\s*Beschreibung:\\s*([^;]+);\\s*Empfehlung:\\s*(.*)";
        Pattern pattern = Pattern.compile(regex);
        String[] lines = reportText.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().startsWith("Severity:")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String severity = matcher.group(1).trim();
                    String name = matcher.group(3).trim();
                    String type = matcher.group(4).trim();
                    String desc = matcher.group(5).trim();
                    String recommendation = matcher.group(6).trim();

                    String displayType = severity + " (" + type + ")";
                    int confidence = severity.equalsIgnoreCase("FEHLER") ? 99 : 75;
                    String combinedText = desc + " -> " + recommendation;

                    issues.add(new ReviewIssue(name, displayType, combinedText, confidence));
                }
            }
        }

        // 3. Entscheidung
        if (!issues.isEmpty()) {
            return new AnalysisResult(AnalysisResult.Status.ISSUES_FOUND, issues);
        } else {
            return new AnalysisResult(AnalysisResult.Status.PARSING_ERROR, issues);
        }
    }

    public List<ReviewDisplayItem> prepareDisplayData(List<ReviewIssue> rawIssues) {
        List<ReviewDisplayItem> displayItems = new ArrayList<>();
        for (ReviewIssue issue : rawIssues) {
            String displayConfidence = issue.getConfidence() + "%";
            String explanation = "<html><body style='width: 300px;'>" +
                    "<b>Problem:</b> " + issue.getIssueType() + "<br><br>" +
                    "<b>Detail:</b> " + issue.getSuggestion() +
                    "</body></html>";
            displayItems.add(new ReviewDisplayItem(issue.getElementName(), issue.getIssueType(), displayConfidence, explanation));
        }
        return displayItems;
    }
}