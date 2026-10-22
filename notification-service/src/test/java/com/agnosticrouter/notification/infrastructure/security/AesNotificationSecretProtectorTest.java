package com.agnosticrouter.notification.infrastructure.security;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class AesNotificationSecretProtectorTest {
    @Test void credentialsUseRandomizedAuthenticatedEncryptionAndRequireTheSameMasterKey() {
        var protector=new AesNotificationSecretProtector(Base64.getEncoder().encodeToString(new byte[32]));
        String first=protector.encrypt("smtp-password"),second=protector.encrypt("smtp-password");
        assertNotEquals(first,second);assertEquals("smtp-password",protector.decrypt(first));assertFalse(first.contains("smtp-password"));
        byte[] different=new byte[32];different[0]=1;
        assertThrows(IllegalStateException.class,()->new AesNotificationSecretProtector(Base64.getEncoder().encodeToString(different)).decrypt(first));
    }
    @Test void missingMasterKeyCannotPersistPlaintext() {
        var protector=new AesNotificationSecretProtector("");assertFalse(protector.available());assertThrows(IllegalStateException.class,()->protector.encrypt("smtp-password"));
    }
}
