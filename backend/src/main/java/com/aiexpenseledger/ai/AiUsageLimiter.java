package com.aiexpenseledger.ai;

import com.aiexpenseledger.exception.AiQuotaExceededException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Records each AI request and rejects it once the per-user or app-wide daily cap is reached.
 * Counters live in the database, so they survive restarts and work across instances.
 */
@Service
public class AiUsageLimiter {

    private static final String INCREMENT_USER_COUNT = """
            INSERT INTO ai_usage (user_id, usage_date, request_count) VALUES (?, ?, 1)
            ON CONFLICT (user_id, usage_date) DO UPDATE SET request_count = ai_usage.request_count + 1
            RETURNING request_count
            """;
    private static final String GLOBAL_COUNT = "SELECT COALESCE(SUM(request_count), 0) FROM ai_usage WHERE usage_date = ?";

    private final JdbcTemplate jdbcTemplate;
    private final AiUsageProperties properties;

    public AiUsageLimiter(JdbcTemplate jdbcTemplate, AiUsageProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /** Counts one request for {@code userId}; throws if that pushes either daily limit over its cap. */
    @Transactional
    public void acquire(Long userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Integer userCount = jdbcTemplate.queryForObject(INCREMENT_USER_COUNT, Integer.class, userId, today);
        if (userCount != null && userCount > properties.dailyLimitPerUser()) {
            throw new AiQuotaExceededException("You have reached today's limit of " + properties.dailyLimitPerUser()
                    + " AI requests. The limit resets at midnight UTC.");
        }
        Long globalCount = jdbcTemplate.queryForObject(GLOBAL_COUNT, Long.class, today);
        if (globalCount != null && globalCount > properties.dailyLimitGlobal()) {
            throw new AiQuotaExceededException(
                    "The AI assistant has reached its daily usage limit. Please try again tomorrow.");
        }
    }
}
