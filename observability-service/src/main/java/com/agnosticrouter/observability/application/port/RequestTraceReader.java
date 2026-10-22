package com.agnosticrouter.observability.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Read model for reporting; does not expose JDBC. */
public interface RequestTraceReader {
    List<Map<String,Object>> recent(LocalDate start, String timezone);
}
