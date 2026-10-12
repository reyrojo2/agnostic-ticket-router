package com.agnosticrouter.decision;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RuleEvaluator {

    public Optional<KnowledgeModels.RuleConfig> resolveHard(String text, List<KnowledgeModels.RuleConfig> rules) {
        List<KnowledgeModels.RuleConfig> matched = rules.stream()
                .filter(r -> "HARD".equalsIgnoreCase(r.strength()) && matches(text, r))
                .sorted(Comparator.comparingInt(KnowledgeModels.RuleConfig::priority))
                .toList();

        Set<String> targets = new LinkedHashSet<>();
        matched.forEach(r -> targets.add(r.targetOptionCode()));

        if (targets.size() != 1) {
            return Optional.empty();
        }

        String target = targets.iterator().next();
        return matched.stream().filter(r -> target.equals(r.targetOptionCode())).findFirst();
    }

    public List<KnowledgeModels.RuleConfig> resolveSoft(String text, List<KnowledgeModels.RuleConfig> rules) {
        return rules.stream()
                .filter(r -> "SOFT".equalsIgnoreCase(r.strength()) && matches(text, r))
                .sorted(Comparator.comparingInt(KnowledgeModels.RuleConfig::priority))
                .toList();
    }

    private boolean matches(String raw, KnowledgeModels.RuleConfig rule) {
        String text = raw.toLowerCase(Locale.ROOT);
        String pattern = rule.pattern().toLowerCase(Locale.ROOT);

        return switch (rule.ruleType().toUpperCase(Locale.ROOT)) {
            case "CONTAINS_ANY" -> Arrays.stream(pattern.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .anyMatch(text::contains);
            case "CONTAINS_ALL" -> Arrays.stream(pattern.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .allMatch(text::contains);
            case "REGEX" -> Pattern.compile(rule.pattern(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(raw)
                    .find();
            default -> false;
        };
    }
}
