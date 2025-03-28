package com.example.domain.api.ans_api_module.nlp.punctuation_rule.correction;

import com.example.domain.api.ans_api_module.nlp.punctuation_rule.rule.CorrectionRule;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CorrectionPipeline {
    private final List<CorrectionRule> rules;
    private final Map<String, Integer> ruleOrder;

    public CorrectionPipeline(List<CorrectionRule> rules, List<String> ruleOrder) {
        this.ruleOrder = buildOrderMap(ruleOrder);
        this.rules = sortRules(rules);
    }

    public String apply(String text, TextAnalysis analysis) {
        String current = text;
        for (CorrectionRule rule : rules) {
            current = rule.apply(current, analysis);
        }
        return current;
    }

    private Map<String, Integer> buildOrderMap(List<String> ruleNames) {
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ruleNames.size(); i++) {
            order.put(ruleNames.get(i), i);
        }
        return order;
    }

    private List<CorrectionRule> sortRules(List<CorrectionRule> rules) {
        return rules.stream()
                .sorted(Comparator.comparingInt(
                        rule -> ruleOrder.getOrDefault(getRuleName(rule), Integer.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    private String getRuleName(CorrectionRule rule) {
        return rule.getClass().getSimpleName();
    }
}
