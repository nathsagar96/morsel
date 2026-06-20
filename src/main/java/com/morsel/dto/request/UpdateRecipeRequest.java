package com.morsel.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateRecipeRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 10000) String instructions,
        @Size(max = 2048) String imageUrl,
        @NotNull @Size(min = 1, max = 50) List<Long> ingredientIds) {}
