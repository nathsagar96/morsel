package com.morsel.mapper;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.model.Ingredient;
import com.morsel.model.Recipe;
import com.morsel.model.User;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecipeMapper {

    public Recipe toEntity(CreateRecipeRequest request, User author, List<Ingredient> ingredients) {
        return Recipe.builder()
                .title(request.title())
                .description(request.description())
                .instructions(request.instructions())
                .imageUrl(request.imageUrl())
                .author(author)
                .ingredients(ingredients)
                .build();
    }

    public RecipeResponse toResponse(Recipe recipe) {
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

    public void updateEntity(Recipe recipe, UpdateRecipeRequest request, List<Ingredient> ingredients) {
        recipe.setTitle(request.title());
        recipe.setDescription(request.description());
        recipe.setInstructions(request.instructions());
        recipe.setImageUrl(request.imageUrl());
        recipe.setIngredients(ingredients);
    }
}
