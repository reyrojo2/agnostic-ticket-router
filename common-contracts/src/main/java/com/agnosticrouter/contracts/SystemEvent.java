package com.agnosticrouter.contracts;

import java.time.Instant;
import java.util.Map;

public record SystemEvent(
        String requestId,
        String profileCode,
        String recipient,
        String type,
        String message,
        Map<String, Object> data,
        Instant occurredAt
) {}
