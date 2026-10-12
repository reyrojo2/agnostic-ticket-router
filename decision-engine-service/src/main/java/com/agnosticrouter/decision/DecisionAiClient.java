package com.agnosticrouter.decision;

public interface DecisionAiClient {
    SystemOneModels.Response decide(
            Object state,
            String questionId,
            SystemOneModels.Question question,
            double minConfidence
    );
}
