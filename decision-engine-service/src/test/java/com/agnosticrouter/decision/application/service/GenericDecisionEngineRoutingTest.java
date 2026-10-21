package com.agnosticrouter.decision.application.service;

import com.agnosticrouter.decision.application.port.DecisionAiClient;
import com.agnosticrouter.decision.domain.model.KnowledgeModels;
import com.agnosticrouter.decision.application.model.SystemOneModels;
import com.agnosticrouter.decision.application.port.KnowledgeProfileProvider;
import com.agnosticrouter.decision.domain.service.RuleEvaluator;
import com.agnosticrouter.decision.domain.service.HardRuleDecisionResolver;
import com.agnosticrouter.decision.domain.service.DimensionDependencyPolicy;
import com.agnosticrouter.decision.domain.service.DecisionContextBuilder;

import com.agnosticrouter.contracts.IncomingEmailEvent;
import com.agnosticrouter.contracts.StandardTicketEvent;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GenericDecisionEngineRoutingTest {
    private IncomingEmailEvent email() {
        return new IncomingEmailEvent("request-test", "TELCO_DEMO", "test@example.org",
                List.of(), List.of(), "Cambio RUC conectividad", "Solicito cambio RUC",
                Map.of(), Instant.now());
    }

    private KnowledgeModels.DimensionConfig dimension(String code, List<KnowledgeModels.RuleConfig> rules) {
        return new KnowledgeModels.DimensionConfig(code, code, "CHOICE", "Clasifica", 1, true,
                0.8, 0.1, null, null,
                List.of(
                    new KnowledgeModels.OptionConfig("BACKOFFICE","Backoffice","Cambio administrativo", "", "", 1,null,null),
                    new KnowledgeModels.OptionConfig("SOPORTE","Soporte","Ayuda técnica", "", "", 2,null,null)
                ), rules);
    }

    private KnowledgeModels.RuleConfig rule(String code, String strength, String target, String pattern) {
        return new KnowledgeModels.RuleConfig(code,target,"CONTAINS_ANY",pattern,strength,1);
    }

    private GenericDecisionEngine engine(KnowledgeProfileProvider knowledge, DecisionAiClient ai,
            KnowledgeModels.DimensionConfig... dims) {
        when(knowledge.getProfile("TELCO_DEMO")).thenReturn(
            new KnowledgeModels.ProfileConfig("TELCO_DEMO","Telco",1,List.of(dims)));
        RuleEvaluator ruleEvaluator = new RuleEvaluator();
        return new GenericDecisionEngine(knowledge, ruleEvaluator,
                new HardRuleDecisionResolver(ruleEvaluator),
                new AiDimensionEvaluator(ai),
                new DecisionContextBuilder(),
                new DimensionDependencyPolicy());
    }

    @Test void allFourHardDimensionsMakeZeroAiCalls() {
        KnowledgeProfileProvider knowledge=mock(KnowledgeProfileProvider.class);
        DecisionAiClient ai=mock(DecisionAiClient.class);
        GenericDecisionEngine engine=engine(knowledge,ai,
            dimension("DESTINO",List.of(rule("H1","HARD","BACKOFFICE","cambio ruc"))),
            dimension("PRODUCTO",List.of(rule("H2","HARD","BACKOFFICE","conectividad"))),
            dimension("PRIORIDAD",List.of(rule("H3","HARD","BACKOFFICE","cambio ruc"))),
            dimension("TIPO_GESTION",List.of(rule("H4","HARD","BACKOFFICE","cambio ruc"))));
        StandardTicketEvent result=engine.evaluate(email());
        assertEquals(4,result.decisions().size());
        assertTrue(result.decisions().values().stream().allMatch(d->"RULE".equals(d.source())));
        verifyNoInteractions(ai);
    }

    @Test void softRuleDoesNotSuppressAiAndFourUnresolvedDimensionsCanAllUseAi() {
        KnowledgeProfileProvider knowledge=mock(KnowledgeProfileProvider.class);
        DecisionAiClient ai=mock(DecisionAiClient.class);
        var answer=new SystemOneModels.Answer("choice","BACKOFFICE",null,null,
                Map.of("BACKOFFICE",0.95,"SOPORTE",0.05),null,0.95,0.95,false,null);
        for(String code:List.of("DESTINO","PRODUCTO","PRIORIDAD","TIPO_GESTION")) {
            when(ai.decide(any(),eq(code),any(),anyDouble())).thenReturn(
                new SystemOneModels.Response("test",Map.of(code,answer),null,null));
        }
        GenericDecisionEngine engine=engine(knowledge,ai,
            dimension("DESTINO",List.of(rule("S1","SOFT","BACKOFFICE","cambio ruc"))),
            dimension("PRODUCTO",List.of()),
            dimension("PRIORIDAD",List.of()),
            dimension("TIPO_GESTION",List.of()));
        StandardTicketEvent result=engine.evaluate(email());
        assertEquals(4,result.decisions().size());
        assertTrue(result.decisions().values().stream().allMatch(d->"AI".equals(d.source())));
        verify(ai,times(4)).decide(any(),anyString(),any(),anyDouble());
    }

    @Test void mixedHardSoftAndUnruledOnlyInvokeAiForUnresolvedDimensions() {
        KnowledgeProfileProvider knowledge=mock(KnowledgeProfileProvider.class);
        DecisionAiClient ai=mock(DecisionAiClient.class);
        for(String code:List.of("PRIORIDAD","TIPO_GESTION")) {
            var answer=new SystemOneModels.Answer("choice","BACKOFFICE",null,null,
                Map.of("BACKOFFICE",0.95,"SOPORTE",0.05),null,0.95,0.95,false,null);
            when(ai.decide(any(),eq(code),any(),anyDouble())).thenReturn(
                new SystemOneModels.Response("test",Map.of(code,answer),null,null));
        }
        GenericDecisionEngine engine=engine(knowledge,ai,
            dimension("DESTINO",List.of(rule("H1","HARD","BACKOFFICE","cambio ruc"))),
            dimension("PRODUCTO",List.of(rule("H2","HARD","BACKOFFICE","conectividad"))),
            dimension("PRIORIDAD",List.of(rule("S1","SOFT","BACKOFFICE","cambio ruc"))),
            dimension("TIPO_GESTION",List.of()));
        StandardTicketEvent result=engine.evaluate(email());
        assertEquals("RULE",result.decisions().get("DESTINO").source());
        assertEquals("RULE",result.decisions().get("PRODUCTO").source());
        assertEquals("AI",result.decisions().get("PRIORIDAD").source());
        assertEquals("AI",result.decisions().get("TIPO_GESTION").source());
        verify(ai,times(2)).decide(any(),anyString(),any(),anyDouble());
    }

    @Test void unavailableAiReturnsHumanReviewWithoutRepeatedCall() {
        KnowledgeProfileProvider knowledge=mock(KnowledgeProfileProvider.class);
        DecisionAiClient ai=mock(DecisionAiClient.class);
        when(ai.decide(any(),anyString(),any(),anyDouble())).thenThrow(new IllegalStateException("payment required"));
        GenericDecisionEngine engine=engine(knowledge,ai,
            dimension("DESTINO",List.of()),
            dimension("PRODUCTO",List.of()));
        StandardTicketEvent result=engine.evaluate(email());
        assertTrue(result.requiresHumanReview());
        assertEquals("UNAVAILABLE",result.decisions().get("DESTINO").source());
        assertEquals("UNAVAILABLE",result.decisions().get("PRODUCTO").source());
        verify(ai,times(1)).decide(any(),anyString(),any(),anyDouble());
    }
}
