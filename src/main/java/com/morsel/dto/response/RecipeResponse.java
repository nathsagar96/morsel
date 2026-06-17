package com.morsel.dto.response;

import com.morsel.model.Recipe;
import java.time.Instant;
import java.util.List;

public record RecipeResponse(
        Long id,
        String title,
        String description,
        String instructions,
        String imageUrl,
        Long authorId,
        String authorUsername,
        List<Long> ingredientIds,
        Double averageRating,
        Integer ratingCount,
        Instant createdAt,
        Instant updatedAt) {

    public static RecipeResponse of(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getInstructions(),
                recipe.getImageUrl(),
                recipe.getAuthor().getId(),
                recipe.getAuthor().getUsername(),
                recipe.getIngredients().stream().map(i -> i.getId()).toList(),
                recipe.getAverageRating(),
                recipe.getRatingCount(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt());
    }
}
