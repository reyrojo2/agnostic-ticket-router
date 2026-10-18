package com.agnosticrouter.observability.api;

import com.agnosticrouter.observability.service.MetricsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/metrics")
public class MetricsController {
    private final MetricsService metrics;
    public MetricsController(MetricsService metrics){this.metrics=metrics;}
    @GetMapping("/overview")
    public Map<String,Object> overview(){return metrics.overview();}
}
