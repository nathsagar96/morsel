package com.morsel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record IngredientResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Tomato") String name,
        @Schema(example = "2025-06-15T10:30:00Z") Instant createdAt,
        @Schema(example = "2025-06-20T14:45:00Z") Instant updatedAt) {}
