package com.agnosticrouter.decision.infrastructure.security;

import com.agnosticrouter.decision.application.port.AiSecretProtector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class AesAiSecretProtector implements AiSecretProtector {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    public AesAiSecretProtector(@Value("${app.ai-config.encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) { key = null; return; }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encodedKey); } catch (IllegalArgumentException ex) { throw new IllegalStateException("AI_CONFIG_ENCRYPTION_KEY debe ser Base64 de 32 bytes."); }
        if (bytes.length != 32) throw new IllegalStateException("AI_CONFIG_ENCRYPTION_KEY debe contener exactamente 32 bytes.");
        key = new SecretKeySpec(bytes, "AES");
    }
    public boolean available() { return key != null; }
    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, result, 0, nonce.length); System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
            return "v1:" + Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) { throw new IllegalStateException("No se pudo proteger la clave del proveedor."); }
    }
    public String decrypt(String ciphertext) {
        requireKey();
        try {
            if (!ciphertext.startsWith("v1:")) throw new IllegalArgumentException();
            byte[] value = Base64.getDecoder().decode(ciphertext.substring(3));
            if (value.length < 28) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(value, 12)));
            return new String(cipher.doFinal(value, 12, value.length - 12), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("No se pudo descifrar la clave guardada. Revisa la clave maestra del servidor."); }
    }
    private void requireKey() { if (key == null) throw new IllegalStateException("El servidor debe configurar AI_CONFIG_ENCRYPTION_KEY para guardar claves de IA."); }
}
