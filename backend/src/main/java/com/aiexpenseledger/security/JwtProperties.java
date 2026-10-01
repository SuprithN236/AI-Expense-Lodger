package com.aiexpenseledger.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secret     Base64-encoded HMAC key (at least 256 bits). Blank means "generate an ephemeral key".
 * @param expiration lifetime of issued tokens.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (expiration == null) {
            expiration = Duration.ofHours(12);
        }
    }
}
