package com.agnosticrouter.crm.application.model;

public record CrmExecutionResult(
        boolean success,
        int statusCode,
        String externalId,
        String responseBody,
        int attempts,
        String error,
        String externalNumber
) {
    public CrmExecutionResult(boolean success, int statusCode, String externalId, String responseBody, int attempts, String error) {
        this(success, statusCode, externalId, responseBody, attempts, error, null);
    }
    public String ticketReference() {
        if (externalNumber != null && !externalNumber.isBlank()) return externalNumber;
        return externalId;
    }
}
