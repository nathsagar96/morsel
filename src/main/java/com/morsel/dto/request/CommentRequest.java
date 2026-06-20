package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank
        @Size(max = 2000)
        @Schema(example = "This recipe is absolutely delicious! I made it for my family and everyone loved it.")
        String text) {}
