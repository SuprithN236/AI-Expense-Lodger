package com.aiexpenseledger.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Caps on LLM requests per UTC day. Every AI call costs money, so both limits apply.
 *
 * @param dailyLimitPerUser requests a single user may make per day
 * @param dailyLimitGlobal  requests all users together may make per day
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiUsageProperties(int dailyLimitPerUser, int dailyLimitGlobal) {

    public AiUsageProperties {
        if (dailyLimitPerUser <= 0) dailyLimitPerUser = 20;
        if (dailyLimitGlobal <= 0) dailyLimitGlobal = 200;
    }
}
