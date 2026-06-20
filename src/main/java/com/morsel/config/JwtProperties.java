package com.morsel.config;

import com.morsel.constants.AppPropertyKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = AppPropertyKeys.JWT_PREFIX)
public record JwtProperties(
        @NotBlank String secret,
        @Positive long expirationMs,
        @Positive long refreshExpirationMs,
        @NotBlank String issuer,
        @NotBlank String audience) {}
