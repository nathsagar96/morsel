package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String token,

        @NotBlank @Size(min = 6, max = 100) @Schema(example = "newSecureP@ss1")
        String newPassword) {}
