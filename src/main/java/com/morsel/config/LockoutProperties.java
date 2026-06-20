package com.morsel.config;

import com.morsel.constants.AppPropertyKeys;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = AppPropertyKeys.LOCKOUT_PREFIX)
public record LockoutProperties(
        @Min(1) int maxFailedAttempts, @Min(1) int lockDurationMinutes) {}
