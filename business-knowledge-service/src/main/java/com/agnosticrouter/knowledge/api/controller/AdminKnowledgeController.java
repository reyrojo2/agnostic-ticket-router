package com.agnosticrouter.knowledge.api.controller;


import com.agnosticrouter.knowledge.domain.model.DecisionDimension;
import com.agnosticrouter.knowledge.domain.model.DecisionOption;
import com.agnosticrouter.knowledge.domain.model.DecisionProfile;
import com.agnosticrouter.knowledge.domain.model.DecisionRule;
import com.agnosticrouter.knowledge.application.service.KnowledgeAdminService;
import com.agnosticrouter.knowledge.application.service.KnowledgeAdminService.*;
import com.agnosticrouter.knowledge.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminKnowledgeController {
    private final KnowledgeAdminService service;
    public AdminKnowledgeController(KnowledgeAdminService service) { this.service = service; }
    @GetMapping("/profiles")
    public List<DecisionProfile> profiles() {
        return service.profiles();
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionProfile createProfile(@Valid @RequestBody ProfileRequest request) {
        return service.createProfile(request);
    }

    @PutMapping("/profiles/{id}")
    public DecisionProfile updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return service.updateProfile(id, request);
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable Long id) {
        service.deleteProfile(id);
    }

    @GetMapping("/profiles/{profileId}/dimensions")
    public List<DecisionDimension> dimensions(@PathVariable Long profileId) {
        return service.dimensions(profileId);
    }

    @PostMapping("/profiles/{profileId}/dimensions")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionDimension createDimension(@PathVariable Long profileId, @Valid @RequestBody DimensionRequest request) {
        return service.createDimension(profileId, request);
    }

    @PutMapping("/dimensions/{id}")
    public DecisionDimension updateDimension(@PathVariable Long id, @Valid @RequestBody DimensionRequest request) {
        return service.updateDimension(id, request);
    }

    @DeleteMapping("/dimensions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDimension(@PathVariable Long id) {
        service.deleteDimension(id);
    }

    @GetMapping("/dimensions/{dimensionId}/options")
    public List<DecisionOption> options(@PathVariable Long dimensionId) {
        return service.options(dimensionId);
    }

    @PostMapping("/dimensions/{dimensionId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionOption createOption(@PathVariable Long dimensionId, @Valid @RequestBody OptionRequest request) {
        return service.createOption(dimensionId, request);
    }

    @PutMapping("/options/{id}")
    public DecisionOption updateOption(@PathVariable Long id, @Valid @RequestBody OptionRequest request) {
        return service.updateOption(id, request);
    }

    @DeleteMapping("/options/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOption(@PathVariable Long id) {
        service.deleteOption(id);
    }

    @GetMapping("/dimensions/{dimensionId}/rules")
    public List<DecisionRule> rules(@PathVariable Long dimensionId) {
        return service.rules(dimensionId);
    }

    @PostMapping("/dimensions/{dimensionId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionRule createRule(@PathVariable Long dimensionId, @Valid @RequestBody RuleRequest request) {
        return service.createRule(dimensionId, request);
    }

    @PutMapping("/rules/{id}")
    public DecisionRule updateRule(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        return service.updateRule(id, request);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        service.deleteRule(id);
    }

}
