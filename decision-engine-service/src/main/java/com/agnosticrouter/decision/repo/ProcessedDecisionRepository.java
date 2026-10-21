package com.agnosticrouter.decision.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Owned exclusively by Decision Engine; does not query other service schemas. */
@Repository
public class ProcessedDecisionRepository {
    private final JdbcTemplate jdbc;
    public ProcessedDecisionRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public boolean isProcessed(String requestId) {
        Integer count=jdbc.queryForObject(
            "SELECT count(*) FROM decision_engine.processed_request WHERE request_id=?",
            Integer.class, requestId);
        return count != null && count > 0;
    }
    public void markProcessed(String requestId) {
        jdbc.update("INSERT INTO decision_engine.processed_request(request_id) VALUES (?) ON CONFLICT DO NOTHING",requestId);
    }
}
