package com.agnosticrouter.crm.service;

public record CrmExecutionResult(
        boolean success,
        int statusCode,
        String externalId,
        String responseBody,
        int attempts,
        String error
) {}
