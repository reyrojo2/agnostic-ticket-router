package com.agnosticrouter.gmail.application.port;

/** Non-secret interface for settings used by the Gmail workflow. */
public interface GmailRuntimeSettings {
    String clientId();
    String redirectUri();
    int pollSeconds();
    boolean configured();
}
