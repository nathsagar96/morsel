package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Schema(example = "janedoe") String usernameOrEmail,
        @NotBlank @Schema(example = "secureP@ss1") String password) {}
