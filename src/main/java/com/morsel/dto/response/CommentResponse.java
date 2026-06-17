package com.morsel.dto.response;

import com.morsel.model.Comment;
import java.time.Instant;

public record CommentResponse(Long id, String text, Long userId, String username, Long recipeId, Instant createdAt) {

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
