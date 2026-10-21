package com.agnosticrouter.notification.api;

import com.agnosticrouter.notification.service.NotificationAdminService;
import com.agnosticrouter.notification.service.NotificationAdminService.*;
import com.agnosticrouter.notification.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {
    private final NotificationAdminService service;
    public NotificationAdminController(NotificationAdminService service) { this.service = service; }
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
