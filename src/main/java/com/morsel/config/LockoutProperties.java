package com.morsel.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.lockout")
public record LockoutProperties(
        @Min(1) int maxFailedAttempts, @Min(1) int lockDurationMinutes) {}
