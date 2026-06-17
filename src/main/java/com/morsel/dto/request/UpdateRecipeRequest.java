package com.morsel.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateRecipeRequest(
        @NotBlank String title,
        String description,
        @NotBlank String instructions,
        String imageUrl,
        List<Long> ingredientIds) {}
