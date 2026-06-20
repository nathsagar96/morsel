package com.morsel.config;

import com.morsel.constants.AppPropertyKeys;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = AppPropertyKeys.CORS_PREFIX)
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins,
        @NotEmpty List<String> allowedMethods,
        @NotEmpty List<String> allowedHeaders) {}
