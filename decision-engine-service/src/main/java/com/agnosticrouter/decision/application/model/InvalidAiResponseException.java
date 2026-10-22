package com.agnosticrouter.decision.application.model;

/** Indicates a missing or invalid SystemOne answer, without retaining its remote payload. */
public class InvalidAiResponseException extends RuntimeException {
    public InvalidAiResponseException() { super("Invalid SystemOne answer"); }
}
