package com.aiexpenseledger.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * Where to forward operational events. Disabled by default so deployments without a Log Monitoring
 * Engine (e.g. Render) never try to reach one.
 */
@ConfigurationProperties(prefix = "app.log-monitoring")
public record LogMonitoringProperties(boolean enabled, URI url, String apiKey, Duration timeout, int maxInFlight) {

    public LogMonitoringProperties {
        url = url != null ? url : URI.create("http://localhost:8080/api/v1/logs/submit");
        apiKey = apiKey != null ? apiKey : "";
        timeout = timeout != null ? timeout : Duration.ofSeconds(2);
        maxInFlight = maxInFlight > 0 ? maxInFlight : 50;
    }
}
