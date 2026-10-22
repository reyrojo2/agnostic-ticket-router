package com.agnosticrouter.notification.application.port;

public interface NotificationSecretProtector {
    boolean available();
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
