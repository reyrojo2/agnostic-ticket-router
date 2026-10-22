package com.agnosticrouter.decision.infrastructure.security;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class AesAiSecretProtectorTest {
    @Test void encryptsWithDifferentNoncesAndRejectsWrongMasterKey() {
        String encoded = Base64.getEncoder().encodeToString(new byte[32]);
        var protector = new AesAiSecretProtector(encoded);
        String first = protector.encrypt("test-provider-key"), second = protector.encrypt("test-provider-key");
        assertNotEquals(first, second); assertFalse(first.contains("test-provider-key"));
        assertEquals("test-provider-key", protector.decrypt(first));
        byte[] other = new byte[32]; other[0] = 1;
        var wrong = new AesAiSecretProtector(Base64.getEncoder().encodeToString(other));
        assertThrows(IllegalStateException.class, () -> wrong.decrypt(first));
        byte[] corrupted = Base64.getDecoder().decode(first.substring(3)); corrupted[corrupted.length - 1] ^= 1;
        assertThrows(IllegalStateException.class, () -> protector.decrypt("v1:" + Base64.getEncoder().encodeToString(corrupted)));
    }
    @Test void missingMasterKeyDoesNotAllowPlaintextStorage() {
        var protector = new AesAiSecretProtector("");
        assertFalse(protector.available());
        assertThrows(IllegalStateException.class, () -> protector.encrypt("test-key"));
    }
}
