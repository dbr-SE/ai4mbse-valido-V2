package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.FormFieldDefinition;
import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.util.List;
import java.util.function.Consumer;

public interface UiController {
    // Review Tab
    List<ReviewDisplayItem> handleDisplayRequest(String reviewType);

    // NEU: Startet Review aus dem Dropdown und gibt Ergebnisse zurück
    void handleRunReviewFromTab(RuleDefinition rule, Consumer<List<ReviewDisplayItem>> resultCallback);

    // Regel Tab
    List<FormFieldDefinition> requestRuleFormStructure();
    void handleSaveRuleRequest(RuleDefinition rule);
    List<RuleDefinition> handleLoadRulesRequest();

    // Regel löschen
    void handleDeleteRuleRequest(RuleDefinition rule);

    // Einzelne Regel ausführen (Play-Button)
    void handleRunSingleRuleRequest(RuleDefinition rule);
}