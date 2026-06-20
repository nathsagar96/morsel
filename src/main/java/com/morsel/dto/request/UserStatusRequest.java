package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull @Schema(example = "true") Boolean enabled) {}
