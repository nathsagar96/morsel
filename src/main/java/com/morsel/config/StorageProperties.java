package com.morsel.config;

import com.morsel.constants.AppPropertyKeys;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = AppPropertyKeys.STORAGE_PREFIX)
public record StorageProperties(@NotBlank String uploadDir) {}
