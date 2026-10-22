package com.agnosticrouter.knowledge.api.controller;
import com.agnosticrouter.knowledge.application.service.RuntimeConfigService;
import com.agnosticrouter.knowledge.application.model.RuntimeModels;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/runtime/profiles")
public class RuntimeConfigController {
 private final RuntimeConfigService service; public RuntimeConfigController(RuntimeConfigService s){service=s;}
 @GetMapping("/{code}") public RuntimeModels.ProfileConfig get(@PathVariable String code){ return service.get(code); }
}
