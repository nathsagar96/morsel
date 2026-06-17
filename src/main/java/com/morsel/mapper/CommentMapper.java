package com.morsel.mapper;

import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.model.Comment;
import com.morsel.model.Recipe;
import com.morsel.model.User;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest request, Recipe recipe, User user) {
        return Comment.builder().text(request.text()).recipe(recipe).user(user).build();
    }

    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.of(comment);
    }
}
