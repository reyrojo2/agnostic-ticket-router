package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.model.*;
import com.agnosticrouter.decision.application.port.DecisionAiClient;
import com.agnosticrouter.decision.domain.model.KnowledgeModels;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiDimensionEvaluatorTest {
    private final DecisionAiClient ai=mock(DecisionAiClient.class);
    private final AiDimensionEvaluator evaluator=new AiDimensionEvaluator(ai);
    private KnowledgeModels.DimensionConfig dimension(String type) {
        return new KnowledgeModels.DimensionConfig("AREA","Área de atención",type,"Decide",1,true,.8,.2,null,null,
                List.of(new KnowledgeModels.OptionConfig("SOPORTE","Soporte","Ayuda","","",1,null,null),
                        new KnowledgeModels.OptionConfig("VENTAS","Ventas","Compras","","",2,null,null)),List.of());
    }
    private com.agnosticrouter.contracts.DecisionValue evaluate(String type,SystemOneModels.Answer answer,SystemOneModels.Usage usage) {
        when(ai.decide(any(),eq("AREA"),any(),anyDouble())).thenReturn(new SystemOneModels.Response("test",Map.of("AREA",answer),usage,null));
        return evaluator.evaluate(dimension(type),dimension(type).options(),Map.of(),List.of());
    }
    @Test void scoreUsesStandardSystemOneConfidenceInsteadOfTreatingItAsZero() {
        var value=evaluate("SCORE",new SystemOneModels.Answer("score",null,2.9,null,Map.of(),null,.95,null,null,null),null);
        assertTrue(value.accepted()); assertEquals(.95,value.probability()); assertEquals("AI_ACCEPTED",value.diagnostic().code());
    }
    @Test void distinguishesLowConfidenceAndLowMarginWithTheOriginalThresholds() {
        var low=evaluate("CHOICE",new SystemOneModels.Answer("choice","SOPORTE",null,null,Map.of("SOPORTE",.6,"VENTAS",.4),null,.6,null,false,null),null);
        assertFalse(low.accepted()); assertEquals("AI_LOW_CONFIDENCE",low.diagnostic().code());
        var close=evaluate("CHOICE",new SystemOneModels.Answer("choice","SOPORTE",null,null,Map.of("SOPORTE",.85,"VENTAS",.75),null,.85,null,false,null),null);
        assertFalse(close.accepted()); assertEquals("AI_LOW_MARGIN",close.diagnostic().code());
        assertEquals(.2,close.diagnostic().minimumMargin()); assertEquals("Soporte",close.diagnostic().valueName());
    }
    @Test void rejectsUnknownAnswersAndTruncatedInputEvenWithHighConfidence() {
        var unknown=evaluate("CHOICE",new SystemOneModels.Answer("choice","INVENTADA",null,null,Map.of("INVENTADA",1.0),null,1.0,null,false,null),null);
        assertFalse(unknown.accepted()); assertEquals("AI_INVALID_OPTION",unknown.diagnostic().code());
        var cut=evaluate("CHOICE",new SystemOneModels.Answer("choice","SOPORTE",null,null,Map.of("SOPORTE",1.0),null,1.0,null,false,null),new SystemOneModels.Usage(10,2,true,null));
        assertFalse(cut.accepted()); assertEquals("AI_TRUNCATED",cut.diagnostic().code());
    }
    @Test void missingYesNoProbabilityIsNotInventedAsConfidentNo() {
        assertThrows(InvalidAiResponseException.class,()->evaluate("NOUL",new SystemOneModels.Answer("noul",null,null,null,null,null,null,null,null,null),null));
    }
}
