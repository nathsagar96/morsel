package com.morsel.mapper;

import com.morsel.dto.request.IngredientRequest;
import com.morsel.dto.response.IngredientResponse;
import com.morsel.model.Ingredient;
import org.springframework.stereotype.Component;

@Component
public class IngredientMapper {

    public Ingredient toEntity(IngredientRequest request) {
        return Ingredient.builder().name(request.name()).build();
    }

    public IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(), ingredient.getName(), ingredient.getCreatedAt(), ingredient.getUpdatedAt());
    }
}
