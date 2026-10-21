package com.agnosticrouter.decision.application.port;

/** Idempotency state owned by Decision Engine, never by observability. */
public interface ProcessedDecisionStore {
    boolean isProcessed(String requestId);
    void markProcessed(String requestId);
}
