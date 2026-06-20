package com.morsel.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record RecipeResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Classic Margherita Pizza") String title,

        @Schema(example = "A traditional Neapolitan pizza with fresh mozzarella, tomatoes, and basil.")
        String description,

        @Schema(
                example =
                        "1. Preheat oven to 500°F.\n2. Stretch dough into a 12-inch round.\n3. Spread tomato sauce.\n4. Add mozzarella slices.\n5. Bake for 8-10 minutes.\n6. Garnish with fresh basil.")
        String instructions,

        @Schema(example = "https://example.com/images/margherita.jpg")
        String imageUrl,

        @Schema(example = "1") Long authorId,
        @Schema(example = "chef_john") String authorUsername,
        @Schema(example = "[1, 2, 3, 4, 5]") List<Long> ingredientIds,
        @Schema(example = "4.5") Double averageRating,
        @Schema(example = "23") Integer ratingCount,
        @Schema(example = "2025-06-15T10:30:00Z") Instant createdAt,
        @Schema(example = "2025-06-20T14:45:00Z") Instant updatedAt) {}
