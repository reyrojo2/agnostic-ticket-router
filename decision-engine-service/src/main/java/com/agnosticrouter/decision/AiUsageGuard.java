package com.agnosticrouter.decision;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Persisted quota across container restarts. A reservation is counted even when Laya fails. */
@Component
public class AiUsageGuard {
    private final JdbcTemplate jdbc;
    private final DecisionProperties props;

    public AiUsageGuard(JdbcTemplate jdbc, DecisionProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    public boolean enabled() { return props.ai().enabled(); }
    public int maxCallsPerMessage() { return Math.max(0, props.ai().maxCallsPerMessage()); }

    public SystemOneModels.Response decide(DecisionAiClient client, Object state,
                                            String dimension, SystemOneModels.Question question,
                                            double confidence) {
        if (!enabled() || maxCallsPerMessage() == 0 || props.ai().maxCallsPerDay() <= 0) {
            throw new IllegalStateException("AI disabled");
        }
        int granted = jdbc.update("""
            INSERT INTO decision_engine.ai_daily_usage(day, calls) VALUES (?, 1)
            ON CONFLICT(day) DO UPDATE SET calls = decision_engine.ai_daily_usage.calls + 1
            WHERE decision_engine.ai_daily_usage.calls < ?
            """, java.sql.Date.valueOf(LocalDate.now(ZoneOffset.UTC)), props.ai().maxCallsPerDay());
        if (granted != 1) throw new IllegalStateException("Daily AI budget exhausted");
        return client.decide(state, dimension, question, confidence);
    }
}
