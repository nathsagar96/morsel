package com.morsel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRecipeRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank String instructions,
        String imageUrl,
        List<Long> ingredientIds) {}
