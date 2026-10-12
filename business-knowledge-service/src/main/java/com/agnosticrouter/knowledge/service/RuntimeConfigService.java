package com.agnosticrouter.knowledge.service;

import com.agnosticrouter.knowledge.api.RuntimeModels.*;
import com.agnosticrouter.knowledge.domain.*;
import com.agnosticrouter.knowledge.repo.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RuntimeConfigService {
 private final DecisionProfileRepository profiles; private final DecisionDimensionRepository dimensions; private final DecisionOptionRepository options; private final DecisionRuleRepository rules;
 public RuntimeConfigService(DecisionProfileRepository p,DecisionDimensionRepository d,DecisionOptionRepository o,DecisionRuleRepository r){profiles=p;dimensions=d;options=o;rules=r;}
 public ProfileConfig get(String profileCode){
   DecisionProfile p=profiles.findByCodeAndActiveTrue(profileCode).orElseThrow(() -> new NoSuchElementException("Perfil no encontrado: "+profileCode));
   List<DimensionConfig> ds=dimensions.findByProfileIdAndActiveTrueOrderByExecutionOrderAsc(p.getId()).stream().map(d -> new DimensionConfig(
      d.getCode(),d.getName(),d.getPrimitiveType(),d.getInstructions(),d.getExecutionOrder(),d.getRequired(),d.getAutoMinProbability(),d.getMinMargin(),d.getDependsOnDimensionCode(),d.getDependsOnOptionCode(),
      options.findByDimensionIdAndActiveTrueOrderByPriorityAsc(d.getId()).stream().map(o -> new OptionConfig(o.getCode(),o.getName(),o.getDescription(),o.getIncludeContext(),o.getExcludeContext(),o.getPriority(),o.getDependsOnDimensionCode(),o.getDependsOnOptionCode())).toList(),
      rules.findByDimensionIdAndActiveTrueOrderByPriorityAsc(d.getId()).stream().map(r -> new RuleConfig(r.getCode(),r.getTargetOptionCode(),r.getRuleType(),r.getPattern(),r.getStrength(),r.getPriority())).toList()
   )).toList();
   return new ProfileConfig(p.getCode(),p.getName(),p.getVersion(),ds);
 }
}
