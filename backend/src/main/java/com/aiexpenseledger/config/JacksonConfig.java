package com.aiexpenseledger.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;

@Configuration
public class JacksonConfig {

    /**
     * Money leaves the API as plain decimal strings ("12.50"), so JavaScript clients never coerce it into
     * a binary floating-point number. Incoming strings or numbers are both parsed into BigDecimal.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsPlainString() {
        return builder -> builder.serializerByType(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeString(value.toPlainString());
            }
        });
    }
}
