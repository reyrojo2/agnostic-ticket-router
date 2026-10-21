package com.agnosticrouter.decision;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiUsageGuardTest {
 @Test void disabledModeNeverCallsRemoteProvider() {
   JdbcTemplate jdbc=mock(JdbcTemplate.class);
   DecisionProperties props=new DecisionProperties(
       new DecisionProperties.Knowledge("http://localhost"),
       new DecisionProperties.Ai("laya","https://example.invalid","", "multilingual","es",false,1,10));
   AiUsageGuard guard=new AiUsageGuard(jdbc,props);
   DecisionAiClient ai=mock(DecisionAiClient.class);
   assertThrows(IllegalStateException.class,()->guard.decide(ai, "state", "TIPO",
       new SystemOneModels.Question("choice","",null),0.8));
   verifyNoInteractions(jdbc,ai);
 }
 @Test void zeroDailyBudgetNeverCallsProvider() {
   JdbcTemplate jdbc=mock(JdbcTemplate.class);
   DecisionProperties props=new DecisionProperties(
       new DecisionProperties.Knowledge("http://localhost"),
       new DecisionProperties.Ai("laya","https://example.invalid","", "multilingual","es",true,1,0));
   AiUsageGuard guard=new AiUsageGuard(jdbc,props);
   DecisionAiClient ai=mock(DecisionAiClient.class);
   assertThrows(IllegalStateException.class,()->guard.decide(ai, "state", "TIPO",
       new SystemOneModels.Question("choice","",null),0.8));
   verifyNoInteractions(jdbc,ai);
 }
}
