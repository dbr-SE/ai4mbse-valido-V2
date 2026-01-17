package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.FormFieldDefinition;
import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.util.List;
import java.util.function.Consumer;

public interface UiController {

    // --- NEU: Ergebnis-Container für saubere Kommunikation ---
    class ReviewResult {
        public enum Status { SUCCESS, FAILURE, ISSUES_FOUND }

        private final Status status;
        private final String message;         // Für Popup-Nachrichten
        private final List<ReviewDisplayItem> items; // Für die Tabelle

        public ReviewResult(Status status, String message, List<ReviewDisplayItem> items) {
            this.status = status;
            this.message = message;
            this.items = items;
        }

        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public List<ReviewDisplayItem> getItems() { return items; }
    }
    // ---------------------------------------------------------

    List<FormFieldDefinition> requestRuleFormStructure();
    void handleSaveRuleRequest(RuleDefinition ruleInput);
    void handleDeleteRuleRequest(RuleDefinition rule);
    List<RuleDefinition> handleLoadRulesRequest();
    List<RuleDefinition> handleLoadStandardRulesRequest();

    // WICHTIG: Signatur geändert -> Nimmt jetzt ReviewResult entgegen
    void handleRunReviewFromTab(RuleDefinition rule, Consumer<ReviewResult> resultCallback);

    void handleRunSingleRuleRequest(RuleDefinition rule);
    List<ReviewDisplayItem> handleDisplayRequest(String reviewType);
    void handleManualApiKeySubmit(String key);
    void handleManualExportRequest();
}