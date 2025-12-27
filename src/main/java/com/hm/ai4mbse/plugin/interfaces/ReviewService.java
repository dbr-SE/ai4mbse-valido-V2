package com.hm.ai4mbse.plugin.interfaces;
import com.hm.ai4mbse.plugin.model.ReviewIssue;
import com.hm.ai4mbse.plugin.model.RuleDefinition;
import java.util.List;

public interface ReviewService {
    void createRule(RuleDefinition ruleDefinition);
    List<ReviewIssue> performReview(String reviewType);
}