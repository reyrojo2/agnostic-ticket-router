package com.agnosticrouter.observability.application.service;

import com.agnosticrouter.observability.infrastructure.persistence.RequestTraceRepository;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final RequestTraceRepository repository;
    private final MetricsReportBuilder reports;
    private final ZoneId timezone;

    public MetricsService(RequestTraceRepository repository, MetricsReportBuilder reports,
                          @Value("${app.report-timezone:America/Lima}") String timezone) {
        this.repository=repository;
        this.reports=reports;
        this.timezone=ZoneId.of(timezone);
    }

    public Map<String,Object> overview() {
        LocalDate today=LocalDate.now(timezone);
        return reports.build(repository.recent(today.minusDays(6),timezone.getId()),timezone,today);
    }
}
