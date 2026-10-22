package com.agnosticrouter.gmail.application.model;

import java.time.Instant;

/** Stored connection state; infrastructure handles encryption and SQL. */
public record GmailAccount(String email, String refreshToken, String accessToken, long expiresAt,
        String profileCode, String query, boolean enabled, Instant lastSync, String lastError) {}
