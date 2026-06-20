package com.morsel.dto.response;

import java.time.Instant;
import java.util.List;

public record RecipeSummaryResponse(
        Long id,
        String title,
        String description,
        String imageUrl,
        Long authorId,
        String authorUsername,
        List<Long> ingredientIds,
        Double averageRating,
        Integer ratingCount,
        Instant createdAt,
        Instant updatedAt) {}
