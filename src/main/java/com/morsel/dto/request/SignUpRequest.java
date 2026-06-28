package com.morsel.dto.request;

import com.morsel.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Size(min = 3, max = 50) @Schema(example = "janedoe")
        String username,

        @NotBlank @Email @Size(max = 100) @Schema(example = "jane.doe@example.com")
        String email,

        @NotBlank @StrongPassword @Size(max = 100) @Schema(example = "secureP@ss1")
        String password) {}
