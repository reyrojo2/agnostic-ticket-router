package com.agnosticrouter.observability.application.service;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MetricsReportBuilderTest {
    private final LocalDate today=LocalDate.of(2026,10,22);
    private Map<String,Object> row(String id,String state,Map<String,Object> decisions,int rules,int ai,int rejected) {
        return new LinkedHashMap<>(Map.of("requestId",id,"at",Instant.parse("2026-10-22T06:51:54Z"),"state",state,"rules",rules,"ai",ai,"rejected",rejected,"decisions",decisions));
    }
    private Map<String,Object> decision(String source,boolean accepted,String code,String message) {
        var value=new LinkedHashMap<String,Object>();value.put("source",source);value.put("accepted",accepted);
        if(code!=null)value.put("diagnostic",Map.of("code",code,"message",message,"dimensionName","Área de atención","required",true));
        return value;
    }
    @Test void unavailableAiIsNotLowConfidenceAndMixedRuleFailureIsNotRuleOnly() {
        var unavailable=row("failure","REVIEW_REQUIRED",Map.of("AREA",decision("UNAVAILABLE",false,"AI_MODEL_REQUIRED","Completa Modelo"),"PRODUCTO",decision("RULE",true,null,null)),1,0,1);
        var low=row("uncertain","REVIEW_REQUIRED",Map.of("AREA",decision("AI",false,"AI_LOW_MARGIN","Alternativas cercanas")),0,1,1);
        var rules=row("rules","CRM_CREATED",Map.of("AREA",decision("RULE",true,null,null)),1,0,0);
        var report=new MetricsReportBuilder().build(List.of(unavailable,low,rules),ZoneId.of("America/Lima"),today);
        assertEquals(1,report.get("aiUnavailable"));assertEquals(1,report.get("lowConfidence"));assertEquals(1,report.get("ruleOnly"));
        assertEquals(2,report.get("review"));assertEquals(1,report.get("crmCreated"));
    }
    @Test void historicalUnavailableExplainsItsLimitWithoutInventingTheOriginalHttpError() {
        var trace=row("old","REVIEW_REQUIRED",Map.of("AREA",decision("UNAVAILABLE",false,null,null)),0,0,1);
        var explained=RequestExplanationBuilder.explain(trace);
        assertTrue(explained.get("summary").toString().contains("histórica"));
        assertFalse(explained.get("summary").toString().contains("422"));
        var issues=(List<?>)explained.get("issues");var issue=(Map<?,?>)issues.getFirst();
        assertEquals("AI_UNAVAILABLE_LEGACY",issue.get("code"));assertNull(issue.get("providerStatus"));assertEquals("AI",issue.get("action"));
    }
    @Test void summaryPrefersTheOriginalFailureOverAnAlphabeticallyEarlierSkippedDimension() {
        var decisions=new LinkedHashMap<String,Object>();
        decisions.put("DESTINO",decision("UNAVAILABLE",false,"AI_SKIPPED_AFTER_FAILURE","No se repitió la llamada"));
        decisions.put("PRIORIDAD",decision("UNAVAILABLE",false,"AI_MODEL_REQUIRED","Completa Modelo"));
        var explained=RequestExplanationBuilder.explain(row("new","REVIEW_REQUIRED",decisions,0,0,2));
        assertTrue(explained.get("summary").toString().contains("Completa Modelo"));
        assertEquals(2,((List<?>)explained.get("issues")).size());
    }
    @Test void missingOptionsAreKnowledgeConfigurationRatherThanAnAiOutage() {
        var value=decision("UNAVAILABLE",false,"NO_ELIGIBLE_OPTIONS","Configura respuestas");
        var row=row("empty","REVIEW_REQUIRED",Map.of("AREA",value),0,0,1);
        var result=new MetricsReportBuilder().build(List.of(row),ZoneId.of("America/Lima"),today);
        assertEquals(0,result.get("aiUnavailable"));assertEquals(0,result.get("lowConfidence"));
        var issue=(Map<?,?>)((List<?>)RequestExplanationBuilder.explain(row).get("issues")).getFirst();
        assertEquals("KNOWLEDGE",issue.get("action"));
    }
}
