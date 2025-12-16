package com.hm.ai4mbse.plugin.modules.review;

import com.hm.ai4mbse.plugin.interfaces.ReviewService;
import com.hm.ai4mbse.plugin.model.ReviewIssue;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock-Implementierung des Review-Services.
 * Liefert statische Dummy-Daten zurück, um das UI zu testen.
 */
public class MockReviewService implements ReviewService {

    @Override
    public void createRule(RuleDefinition ruleDefinition) {
        System.out.println("LOG [Review]: Rule creation requested for: " + ruleDefinition);
    }

    @Override
    public List<ReviewIssue> performReview(String reviewType) {
        // Eine Liste für Ergebnisse wird vorbereitet.
        List<ReviewIssue> issues = new ArrayList<>();

        // Künstliche Verzögerung wird simuliert (optional).
        System.out.println("LOG [Review]: Analyzing model for type: " + reviewType);

        if ("Muda Check".equals(reviewType)) {
            issues.add(new ReviewIssue("Block: Battery", "Muda (Unused)", "Block wird nirgends referenziert", 95));
            issues.add(new ReviewIssue("Req: MaxSpeed", "Muda (Untraced)", "Kein Trace zu Logical Arch", 88));
        } else if ("Traceability".equals(reviewType)) {
            issues.add(new ReviewIssue("Port: PowerIn", "Missing Type", "Interface definieren", 60));
        } else {
            // Default Dummies
            issues.add(new ReviewIssue("System Context", "General", "Review erfolgreich gestartet", 100));
        }

        return issues;
    }
}