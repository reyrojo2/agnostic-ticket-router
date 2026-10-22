package com.agnosticrouter.notification.api.controller;
import com.agnosticrouter.notification.domain.model.NotificationChannel;
import com.agnosticrouter.notification.domain.model.NotificationTemplate;

import com.agnosticrouter.notification.application.service.NotificationAdminService;
import com.agnosticrouter.notification.application.service.NotificationAdminService.*;
import com.agnosticrouter.notification.application.service.NotificationMessageCatalog;
import com.agnosticrouter.notification.application.service.NotificationPreviewService;
import com.agnosticrouter.notification.application.port.NotificationSecretProtector;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {
    private final NotificationAdminService service;
    private final NotificationMessageCatalog catalog;
    private final NotificationPreviewService previews;
    private final NotificationSecretProtector secrets;
    public NotificationAdminController(NotificationAdminService service,NotificationMessageCatalog catalog,NotificationPreviewService previews,NotificationSecretProtector secrets) { this.service = service;this.catalog=catalog;this.previews=previews;this.secrets=secrets; }
    @GetMapping("/events")
    public List<NotificationMessageCatalog.EventDefinition> events(){return catalog.events();}
    @GetMapping("/settings")
    public java.util.Map<String,Boolean> settings(){return java.util.Map.of("credentialStorageAvailable",secrets.available());}
    @PostMapping("/preview")
    public NotificationPreviewService.Preview preview(@Valid @RequestBody TemplateRequest request){return previews.preview(request);}
    @GetMapping("/channels")
    public List<NotificationChannel> channels() {
        return service.channels();
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationChannel createChannel(@Valid @RequestBody ChannelRequest request) {
        return service.createChannel(request);
    }

    @PutMapping("/channels/{id}")
    public NotificationChannel updateChannel(@PathVariable Long id, @Valid @RequestBody ChannelRequest request) {
        return service.updateChannel(id, request);
    }

    @DeleteMapping("/channels/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(@PathVariable Long id) {
        service.deleteChannel(id);
    }

    @GetMapping("/templates")
    public List<NotificationTemplate> templates() {
        return service.templates();
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationTemplate createTemplate(@Valid @RequestBody TemplateRequest request) {
        return service.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    public NotificationTemplate updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateRequest request) {
        return service.updateTemplate(id, request);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        service.deleteTemplate(id);
    }

}
