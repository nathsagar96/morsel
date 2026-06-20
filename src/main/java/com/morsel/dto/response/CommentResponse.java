package com.morsel.dto.response;

import com.morsel.model.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CommentResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "This recipe is absolutely delicious! I made it for my family and everyone loved it.")
        String text,

        @Schema(example = "1") Long userId,
        @Schema(example = "janedoe") String username,
        @Schema(example = "42") Long recipeId,
        @Schema(example = "2025-06-20T18:30:00Z") Instant createdAt) {

    public static CommentResponse of(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getText(),
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getRecipe().getId(),
                comment.getCreatedAt());
    }
}
