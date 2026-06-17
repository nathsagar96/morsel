package com.morsel.dto.response;

import com.morsel.model.Recipe;
import com.morsel.model.User;
import java.time.Instant;
import java.util.List;

public record UserProfileResponse(Long id, String username, List<RecipeSummary> recipes) {

    public static UserProfileResponse of(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getRecipes().stream().map(RecipeSummary::from).toList());
    }

    public record RecipeSummary(
            Long id, String title, String imageUrl, Double averageRating, Integer ratingCount, Instant createdAt) {

        static RecipeSummary from(Recipe recipe) {
            return new RecipeSummary(
                    recipe.getId(),
                    recipe.getTitle(),
                    recipe.getImageUrl(),
                    recipe.getAverageRating(),
                    recipe.getRatingCount(),
                    recipe.getCreatedAt());
        }
    }
}
