package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.FormFieldDefinition;
import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public interface UiController {

    class ReviewResult {
        public enum Status { SUCCESS, FAILURE, ISSUES_FOUND }
        private final Status status;
        private final String message;
        private final List<ReviewDisplayItem> items;

        public ReviewResult(Status status, String message, List<ReviewDisplayItem> items) {
            this.status = status;
            this.message = message;
            this.items = items;
        }

        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public List<ReviewDisplayItem> getItems() { return items; }
    }

    List<FormFieldDefinition> requestRuleFormStructure();
    void handleSaveRuleRequest(RuleDefinition ruleInput);
    void handleDeleteRuleRequest(RuleDefinition rule);
    List<RuleDefinition> handleLoadRulesRequest();
    List<RuleDefinition> handleLoadStandardRulesRequest();

    void handleRunReviewFromTab(RuleDefinition rule, Consumer<ReviewResult> resultCallback);

    void handleRunSingleRuleRequest(RuleDefinition rule);
    List<ReviewDisplayItem> handleDisplayRequest(String reviewType);
    void handleManualApiKeySubmit(String key);
    void handleManualExportRequest();

    // --- FEATURE 4: Neue Methode für den Report-Export ---
    void handleExportReportRequest(File targetFile, List<ReviewDisplayItem> results);
}