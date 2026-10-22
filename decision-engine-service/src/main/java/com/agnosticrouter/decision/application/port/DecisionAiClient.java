package com.agnosticrouter.decision.application.port;

import com.agnosticrouter.decision.application.model.SystemOneModels;

public interface DecisionAiClient {
    SystemOneModels.Response decide(
            Object state,
            String questionId,
            SystemOneModels.Question question,
            double minConfidence
    );
}
