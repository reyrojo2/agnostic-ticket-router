package com.agnosticrouter.decision.domain.model;
import java.util.List;
public final class KnowledgeModels {
 private KnowledgeModels(){}
 public record ProfileConfig(String code,String name,int version,List<DimensionConfig> dimensions){}
 public record DimensionConfig(String code,String name,String primitiveType,String instructions,int executionOrder,boolean required,double autoMinProbability,double minMargin,String dependsOnDimensionCode,String dependsOnOptionCode,List<OptionConfig> options,List<RuleConfig> rules){}
 public record OptionConfig(String code,String name,String description,String includeContext,String excludeContext,int priority,String dependsOnDimensionCode,String dependsOnOptionCode){}
 public record RuleConfig(String code,String targetOptionCode,String ruleType,String pattern,String strength,int priority){}
}
