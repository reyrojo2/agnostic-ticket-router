package com.agnosticrouter.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StandardTicketEvent(
        String requestId,
        String profileCode,
        int profileVersion,
        String sender,
        List<String> to,
        List<String> cc,
        String subject,
        String body,
        Map<String, Object> metadata,
        Map<String, DecisionValue> decisions,
        boolean requiresHumanReview,
        Instant createdAt
) {}
