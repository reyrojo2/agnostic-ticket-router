package com.agnosticrouter.knowledge.service;

import com.agnosticrouter.knowledge.domain.*;
import com.agnosticrouter.knowledge.repo.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
public class KnowledgeAdminService {
    private final DecisionProfileRepository profiles;
    private final DecisionDimensionRepository dimensions;
    private final DecisionOptionRepository options;
    private final DecisionRuleRepository rules;

    public KnowledgeAdminService(
            DecisionProfileRepository profiles,
            DecisionDimensionRepository dimensions,
            DecisionOptionRepository options,
            DecisionRuleRepository rules
    ) {
        this.profiles = profiles;
        this.dimensions = dimensions;
        this.options = options;
        this.rules = rules;
    }
    public List<DecisionProfile> profiles() {
        return profiles.findAll();
    }
    public DecisionProfile createProfile(ProfileRequest request) {
        DecisionProfile profile = new DecisionProfile();
        apply(profile, request);
        return profiles.save(profile);
    }
    public DecisionProfile updateProfile(Long id, ProfileRequest request) {
        DecisionProfile profile = profiles.findById(id).orElseThrow();
        apply(profile, request);
        return profiles.save(profile);
    }
    public void deleteProfile(Long id) {
        profiles.deleteById(id);
    }
    public List<DecisionDimension> dimensions(Long profileId) {
        return dimensions.findByProfileIdOrderByExecutionOrderAsc(profileId);
    }
    public DecisionDimension createDimension(
            Long profileId,
            DimensionRequest request
    ) {
        DecisionDimension dimension = new DecisionDimension();
        dimension.setProfile(profiles.findById(profileId).orElseThrow());
        apply(dimension, request);
        return dimensions.save(dimension);
    }
    public DecisionDimension updateDimension(Long id, DimensionRequest request) {
        DecisionDimension dimension = dimensions.findById(id).orElseThrow();
        apply(dimension, request);
        return dimensions.save(dimension);
    }
    public void deleteDimension(Long id) {
        dimensions.deleteById(id);
    }
    public List<DecisionOption> options(Long dimensionId) {
        return options.findByDimensionIdOrderByPriorityAsc(dimensionId);
    }
    public DecisionOption createOption(
            Long dimensionId,
            OptionRequest request
    ) {
        DecisionOption option = new DecisionOption();
        option.setDimension(dimensions.findById(dimensionId).orElseThrow());
        apply(option, request);
        return options.save(option);
    }
    public DecisionOption updateOption(Long id, OptionRequest request) {
        DecisionOption option = options.findById(id).orElseThrow();
        apply(option, request);
        return options.save(option);
    }
    public void deleteOption(Long id) {
        options.deleteById(id);
    }
    public List<DecisionRule> rules(Long dimensionId) {
        return rules.findByDimensionIdOrderByPriorityAsc(dimensionId);
    }
    public DecisionRule createRule(
            Long dimensionId,
            RuleRequest request
    ) {
        DecisionRule rule = new DecisionRule();
        rule.setDimension(dimensions.findById(dimensionId).orElseThrow());
        apply(rule, request);
        return rules.save(rule);
    }
    public DecisionRule updateRule(Long id, RuleRequest request) {
        DecisionRule rule = rules.findById(id).orElseThrow();
        apply(rule, request);
        return rules.save(rule);
    }
    public void deleteRule(Long id) {
        rules.deleteById(id);
    }

    private void apply(DecisionProfile profile, ProfileRequest request) {
        profile.setCode(request.code());
        profile.setName(request.name());
        profile.setVersion(request.version() == null ? 1 : request.version());
        profile.setActive(request.active() == null || request.active());
    }

    private void apply(DecisionDimension dimension, DimensionRequest request) {
        dimension.setCode(request.code());
        dimension.setName(request.name());
        dimension.setPrimitiveType(request.primitiveType());
        dimension.setInstructions(request.instructions());
        dimension.setExecutionOrder(request.executionOrder());
        dimension.setRequired(request.required());
        dimension.setAutoMinProbability(request.autoMinProbability());
        dimension.setMinMargin(request.minMargin());
        dimension.setDependsOnDimensionCode(request.dependsOnDimensionCode());
        dimension.setDependsOnOptionCode(request.dependsOnOptionCode());
        dimension.setActive(request.active());
    }

    private void apply(DecisionOption option, OptionRequest request) {
        option.setCode(request.code());
        option.setName(request.name());
        option.setDescription(request.description());
        option.setIncludeContext(request.includeContext());
        option.setExcludeContext(request.excludeContext());
        option.setPriority(request.priority());
        option.setDependsOnDimensionCode(request.dependsOnDimensionCode());
        option.setDependsOnOptionCode(request.dependsOnOptionCode());
        option.setActive(request.active());
    }

    private void apply(DecisionRule rule, RuleRequest request) {
        rule.setCode(request.code());
        rule.setTargetOptionCode(request.targetOptionCode());
        rule.setRuleType(request.ruleType());
        rule.setPattern(request.pattern());
        rule.setStrength(request.strength());
        rule.setPriority(request.priority());
        rule.setActive(request.active());
    }

    public record ProfileRequest(
            @NotBlank String code,
            @NotBlank String name,
            Integer version,
            Boolean active
    ) {}

    public record DimensionRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String primitiveType,
            @NotBlank String instructions,
            Integer executionOrder,
            Boolean required,
            Double autoMinProbability,
            Double minMargin,
            String dependsOnDimensionCode,
            String dependsOnOptionCode,
            Boolean active
    ) {
        public DimensionRequest {
            if (executionOrder == null) executionOrder = 100;
            if (required == null) required = true;
            if (autoMinProbability == null) autoMinProbability = .70;
            if (minMargin == null) minMargin = .20;
            if (active == null) active = true;
        }
    }

    public record OptionRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String description,
            String includeContext,
            String excludeContext,
            Integer priority,
            String dependsOnDimensionCode,
            String dependsOnOptionCode,
            Boolean active
    ) {
        public OptionRequest {
            if (priority == null) priority = 100;
            if (active == null) active = true;
        }
    }

    public record RuleRequest(
            @NotBlank String code,
            @NotBlank String targetOptionCode,
            @NotBlank String ruleType,
            @NotBlank String pattern,
            String strength,
            Integer priority,
            Boolean active
    ) {
        public RuleRequest {
            if (strength == null) strength = "SOFT";
            if (priority == null) priority = 100;
            if (active == null) active = true;
        }
    }
}
