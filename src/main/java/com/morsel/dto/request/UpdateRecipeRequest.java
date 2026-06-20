package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateRecipeRequest(
        @NotBlank @Size(max = 255) @Schema(example = "Classic Margherita Pizza (Thin Crust)")
        String title,

        @Size(max = 5000) @Schema(example = "An updated version with a crispy thin crust.")
        String description,

        @NotBlank
        @Size(max = 10000)
        @Schema(
                example =
                        "1. Preheat oven to 475°F.\n2. Stretch dough thin.\n3. Add sauce and cheese.\n4. Bake for 6-8 minutes.")
        String instructions,

        @Size(max = 2048) @Schema(example = "https://example.com/images/margherita-v2.jpg")
        String imageUrl,

        @NotNull @Size(min = 1, max = 50) @Schema(example = "[1, 2, 3, 4, 5, 6]")
        List<@NotNull @Positive Long> ingredientIds) {}
