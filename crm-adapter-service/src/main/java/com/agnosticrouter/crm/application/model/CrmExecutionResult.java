package com.agnosticrouter.crm.application.model;

public record CrmExecutionResult(
        boolean success,
        int statusCode,
        String externalId,
        String responseBody,
        int attempts,
        String error
) {}
