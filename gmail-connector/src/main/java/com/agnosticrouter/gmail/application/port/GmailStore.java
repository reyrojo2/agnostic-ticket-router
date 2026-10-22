package com.agnosticrouter.gmail.application.port;

import com.agnosticrouter.gmail.application.model.GmailAccount;

/** Storage for Gmail OAuth state, account settings and message deduplication. */
public interface GmailStore {
    GmailAccount account();
    void saveAccount(String email, String refresh, String access, long expiry);
    void access(String token, long expiry);
    void settings(String profile, String query, boolean enabled);
    void synced();
    void failed(String error);
    void disconnect();
    void state(String state);
    boolean consumeState(String state);
    boolean processed(String email, String id);
    void markProcessed(String email, String id, String requestId);
}
