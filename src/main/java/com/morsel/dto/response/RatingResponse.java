package com.morsel.dto.response;

import com.morsel.model.Rating;
import io.swagger.v3.oas.annotations.media.Schema;

public record RatingResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "4") Integer score,
        @Schema(example = "1") Long userId,
        @Schema(example = "janedoe") String username,
        @Schema(example = "42") Long recipeId) {

    public static RatingResponse of(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getUser().getId(),
                rating.getUser().getUsername(),
                rating.getRecipe().getId());
    }
}
