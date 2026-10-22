package com.agnosticrouter.notification.application.service;

import com.agnosticrouter.notification.application.port.*;
import com.agnosticrouter.notification.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NotificationAdminServiceTest {
    private final NotificationChannelStore channels=mock(NotificationChannelStore.class);
    private final NotificationTemplateStore templates=mock(NotificationTemplateStore.class);
    private final NotificationSecretProtector secrets=mock(NotificationSecretProtector.class);
    private final NotificationAdminService service=new NotificationAdminService(channels,templates,secrets,new NotificationMessageCatalog());
    private NotificationAdminService.ChannelRequest command(String user,String password,boolean auth) {
        return new NotificationAdminService.ChannelRequest("MAIL","Correo","SMTP","smtp.example.test",587,"","","sender@example.test",auth,true,true,user,password,false,false);
    }
    @Test void encryptsCredentialsAndNeitherCiphertextNorPlaintextAreReturnedToAngular() throws Exception {
        when(secrets.encrypt("account")).thenReturn("cipher-user");when(secrets.encrypt("secret")).thenReturn("cipher-password");
        when(channels.save(any())).thenAnswer(i->i.getArgument(0));
        var saved=service.createChannel(command("account","secret",true));
        assertEquals("cipher-password",saved.getEncryptedPassword());assertTrue(saved.isUsernameConfigured());assertTrue(saved.isPasswordConfigured());
        String json=new ObjectMapper().writeValueAsString(saved);
        assertFalse(json.contains("cipher-password"));assertFalse(json.contains("cipher-user"));assertFalse(json.contains("secret"));assertFalse(command("account","secret",true).toString().contains("secret"));
    }
    @Test void emptyFieldsPreserveExistingCredentialsAndNoAuthDoesNotRequireThem() {
        var existing=new NotificationChannel();existing.setId(1L);existing.setCode("MAIL");existing.setEncryptedUsername("old-user");existing.setEncryptedPassword("old-password");
        when(channels.findById(1L)).thenReturn(Optional.of(existing));when(templates.findAll()).thenReturn(List.of());when(channels.save(any())).thenAnswer(i->i.getArgument(0));
        var saved=service.updateChannel(1L,command("","",true));assertEquals("old-password",saved.getEncryptedPassword());verifyNoInteractions(secrets);
        assertDoesNotThrow(()->service.createChannel(command("","",false)));
    }
    @Test void cannotDeleteAChannelReferencedByMessages() {
        var existing=new NotificationChannel();existing.setId(1L);existing.setCode("MAIL");
        var template=new NotificationTemplate();template.setChannelCode("MAIL");when(channels.findById(1L)).thenReturn(Optional.of(existing));when(templates.findAll()).thenReturn(List.of(template));
        assertThrows(IllegalArgumentException.class,()->service.deleteChannel(1L));verify(channels,never()).deleteById(anyLong());
    }
    @Test void preventsAnActiveMessageFromUsingADisabledChannel() {
        var channel=new NotificationChannel();channel.setCode("MAIL");channel.setActive(false);when(channels.findAll()).thenReturn(List.of(channel));
        assertThrows(IllegalArgumentException.class,()->service.createTemplate(new NotificationAdminService.TemplateRequest("P","CRM_CREATED","MAIL","{{recipient}}","{{confirmationSubject}}","{{confirmationMessage}}",true)));
        verify(templates,never()).save(any());
    }
}
