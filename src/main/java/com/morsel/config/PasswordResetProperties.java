package com.morsel.config;

import com.morsel.constants.AppPropertyKeys;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = AppPropertyKeys.PASSWORD_RESET_PREFIX)
public record PasswordResetProperties(@Min(1) int tokenExpiryMinutes) {}
