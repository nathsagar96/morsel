package com.morsel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRecipeRequest(
        @NotBlank @Size(max = 255) @Schema(example = "Classic Margherita Pizza")
        String title,

        @Size(max = 5000)
        @Schema(example = "A traditional Neapolitan pizza with fresh mozzarella, tomatoes, and basil.")
        String description,

        @NotBlank
        @Size(max = 10000)
        @Schema(
                example =
                        "1. Preheat oven to 500°F.\n2. Stretch dough into a 12-inch round.\n3. Spread tomato sauce.\n4. Add mozzarella slices.\n5. Bake for 8-10 minutes.\n6. Garnish with fresh basil.")
        String instructions,

        @Size(max = 2048) @Schema(example = "https://example.com/images/margherita.jpg")
        String imageUrl,

        @NotNull @Size(min = 1, max = 50) @Schema(example = "[1, 2, 3, 4, 5]")
        List<@NotNull @Positive Long> ingredientIds) {}
