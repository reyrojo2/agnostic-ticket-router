package com.agnosticrouter.gmail.application.port;

/** Encryption and decryption boundary for persisted OAuth credentials. */
public interface TokenProtector {
    String encrypt(String value);
    String decrypt(String encrypted);
}
