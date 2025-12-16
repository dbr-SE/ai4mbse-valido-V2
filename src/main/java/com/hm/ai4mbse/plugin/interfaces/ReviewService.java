package com.hm.ai4mbse.plugin.interfaces;

import com.hm.ai4mbse.plugin.model.ReviewIssue;
import com.hm.ai4mbse.plugin.model.RuleDefinition;
import java.util.List;

/**
 * Interface für den Review-Service.
 * Definiert Methoden für Regelerstellung und Review-Durchführung.
 */
public interface ReviewService {

    // Bestehende Methode (Regel generieren)
    void createRule(RuleDefinition ruleDefinition);

    /**
     * Ein Review wird basierend auf dem gewählten Typ durchgeführt.
     * @param reviewType Der Name des Reviews (z.B. "Muda").
     * @return Eine Liste von gefundenen Problemen (Issues).
     */
    List<ReviewIssue> performReview(String reviewType);
}