package com.agnosticrouter.contracts;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record IncomingEmailEvent(
        String requestId,
        String profileCode,
        String from,
        List<String> to,
        List<String> cc,
        String subject,
        String body,
        Map<String, Object> metadata,
        Instant receivedAt
) {}
