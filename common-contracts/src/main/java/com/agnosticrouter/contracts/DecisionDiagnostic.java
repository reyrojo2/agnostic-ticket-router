package com.agnosticrouter.contracts;

/** Safe explanation captured at evaluation time; never contains credentials or remote error bodies. */
public record DecisionDiagnostic(
        String code, String message, String dimensionName, String valueName,
        boolean required, Double minimumConfidence, Double minimumMargin, Integer providerStatus
) {}
