package com.agnosticrouter.decision.application.port;

import com.agnosticrouter.decision.application.model.AiConnection;
import com.agnosticrouter.decision.application.model.SystemOneModels;

public interface SystemOneTransport {
    SystemOneModels.Response decide(AiConnection connection, Object state, String questionId,
                                   SystemOneModels.Question question, double minConfidence);
}
