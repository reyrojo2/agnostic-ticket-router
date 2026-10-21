package com.agnosticrouter.democrm.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/tickets")
public class DemoCrmController {
    private final AtomicLong sequence = new AtomicLong(1000);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody Map<String, Object> payload) {
        String caseNumber = "CASE-" + sequence.incrementAndGet();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("caseNumber", caseNumber);
        data.put("createdAt", Instant.now());
        data.put("received", payload);
        return Map.of("success", true, "data", data);
    }
}
