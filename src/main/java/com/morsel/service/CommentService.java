package com.morsel.service;

import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.CommentMapper;
import com.morsel.model.Comment;
import com.morsel.model.Recipe;
import com.morsel.model.User;
import com.morsel.repository.CommentRepository;
import com.morsel.repository.RecipeRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class CommentService {

    private final CommentRepository commentRepository;
    private final RecipeRepository recipeRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse addComment(Long recipeId, CommentRequest request, User user) {
        Recipe recipe = recipeRepository
                .findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        Comment comment = commentMapper.toEntity(request, recipe, user);
        comment = commentRepository.save(comment);
        log.info("Comment added: id={}, recipeId={}, userId={}", comment.getId(), recipeId, user.getId());
        return commentMapper.toResponse(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long recipeId, Pageable pageable) {
        if (!recipeRepository.existsById(recipeId)) {
            throw new ResourceNotFoundException("Recipe not found with id: " + recipeId);
        }
        return commentRepository
                .findByRecipeIdOrderByCreatedAtDesc(recipeId, pageable)
                .map(commentMapper::toResponse);
    }
}
