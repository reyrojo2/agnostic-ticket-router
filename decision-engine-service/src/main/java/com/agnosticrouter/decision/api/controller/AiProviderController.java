package com.agnosticrouter.decision.api.controller;

import com.agnosticrouter.decision.application.service.AiProviderAdminService;
import com.agnosticrouter.decision.application.service.AiProviderProbeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai")
public class AiProviderController {
    private final AiProviderAdminService admin;
    private final AiProviderProbeService probes;
    public AiProviderController(AiProviderAdminService admin, AiProviderProbeService probes) { this.admin = admin; this.probes = probes; }
    @GetMapping("/providers") public List<AiProviderAdminService.ProviderView> list() { return admin.list(); }
    @GetMapping("/settings") public AiProviderAdminService.SettingsView settings() { return admin.settings(); }
    @PostMapping("/providers") @ResponseStatus(HttpStatus.CREATED)
    public AiProviderAdminService.ProviderView create(@RequestBody AiProviderAdminService.SaveProvider request) { return admin.save(null, request); }
    @PutMapping("/providers/{id}")
    public AiProviderAdminService.ProviderView update(@PathVariable Long id, @RequestBody AiProviderAdminService.SaveProvider request) { return admin.save(id, request); }
    @PostMapping("/providers/{id}/select") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void select(@PathVariable Long id) { admin.select(id); }
    @PostMapping("/environment/select") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void environment() { admin.useEnvironment(); }
    @PostMapping("/providers/{id}/test") public AiProviderProbeService.ProbeResult test(@PathVariable Long id) { return probes.test(id); }
    @DeleteMapping("/providers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { admin.delete(id); }
}
