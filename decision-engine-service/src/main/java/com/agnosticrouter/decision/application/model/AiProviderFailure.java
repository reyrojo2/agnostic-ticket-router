package com.agnosticrouter.decision.application.model;

/** Sanitized failure: does not include provider response bodies or credential headers. */
public class AiProviderFailure extends RuntimeException {
    private final int status;
    private final boolean modelRequired;
    public AiProviderFailure(int status) { this(status, false); }
    public AiProviderFailure(int status, boolean modelRequired) {
        super("SystemOne provider failed (HTTP " + status + ")");
        this.status = status; this.modelRequired = modelRequired;
    }
    public int status() { return status; }
    public boolean modelRequired() { return modelRequired; }
}
