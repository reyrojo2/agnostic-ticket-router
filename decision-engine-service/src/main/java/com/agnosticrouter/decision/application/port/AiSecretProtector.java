package com.agnosticrouter.decision.application.port;

public interface AiSecretProtector {
    boolean available();
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
