package com.agnosticrouter.ingestion.domain;

import java.util.List;
import java.util.Map;

/** Validated intake data, independent from transport and HTTP. */
public record EmailIntakeCommand(
        String profileCode,
        String from,
        List<String> to,
        List<String> cc,
        String subject,
        String body,
        Map<String, Object> metadata
) {}
