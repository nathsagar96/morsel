package com.morsel.dto.response;

import com.morsel.model.Rating;

public record RatingResponse(Long id, Integer score, Long userId, String username, Long recipeId) {

    public static RatingResponse of(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getUser().getId(),
                rating.getUser().getUsername(),
                rating.getRecipe().getId());
    }
}
