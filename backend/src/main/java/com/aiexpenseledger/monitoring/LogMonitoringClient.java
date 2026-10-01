package com.aiexpenseledger.monitoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fire-and-forget publisher to the Log Monitoring Engine's {@code POST /api/v1/logs/submit}.
 * Calls never block the request thread and never throw: when the engine is down or slow, events are
 * dropped rather than slowing the app. Only ids and outcomes are sent, never emails or expense text.
 */
@Component
public class LogMonitoringClient {

    public static final String SERVICE_API = "expense-api";
    public static final String SERVICE_AUTH = "expense-auth";
    public static final String SERVICE_LEDGER = "expense-ledger";
    public static final String SERVICE_AI = "expense-ai";

    private static final Logger log = LoggerFactory.getLogger(LogMonitoringClient.class);
    private static final int MAX_MESSAGE_LENGTH = 16_000;

    private final LogMonitoringProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Semaphore inFlight;
    /** Logs the first failure of an outage only, so a stopped engine does not flood the app's own log. */
    private final AtomicBoolean failureReported = new AtomicBoolean();

    public LogMonitoringClient(LogMonitoringProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.inFlight = new Semaphore(properties.maxInFlight());
        this.httpClient = properties.enabled()
                ? HttpClient.newBuilder().connectTimeout(properties.timeout()).build()
                : null;
        if (properties.enabled()) {
            log.info("Forwarding operational events to the Log Monitoring Engine at {}", properties.url());
        }
    }

    public void info(String service, String message) {
        send(service, "INFO", message);
    }

    public void warning(String service, String message) {
        send(service, "WARNING", message);
    }

    public void critical(String service, String message, Throwable error) {
        send(service, "CRITICAL", error == null ? message : message + "\n" + stackTrace(error));
    }

    private void send(String service, String severity, String message) {
        if (httpClient == null) {
            return;
        }
        if (!inFlight.tryAcquire()) {
            log.debug("Log Monitoring Engine backlog full; dropped {} event from {}", severity, service);
            return;
        }
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "serviceName", service,
                    "severity", severity,
                    "logMessage", truncate(message)));
            HttpRequest request = HttpRequest.newBuilder(properties.url())
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, error) -> {
                        inFlight.release();
                        if (error != null) {
                            reportFailure("unreachable (" + error.getClass().getSimpleName() + ")");
                        } else if (response.statusCode() != 202) {
                            reportFailure("rejected an event with HTTP " + response.statusCode());
                        } else {
                            failureReported.set(false);
                        }
                    });
        } catch (JsonProcessingException | RuntimeException e) {
            inFlight.release();
            reportFailure("could not be called (" + e.getMessage() + ")");
        }
    }

    private void reportFailure(String reason) {
        if (failureReported.compareAndSet(false, true)) {
            log.warn("Log Monitoring Engine at {} {}; events are dropped until it recovers", properties.url(), reason);
        }
    }

    private static String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static String truncate(String message) {
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "\n…[truncated]";
    }
}
