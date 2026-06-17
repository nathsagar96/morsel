package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.CommentRequest;
import com.morsel.dto.response.CommentResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.CommentMapper;
import com.morsel.model.Comment;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.CommentRepository;
import com.morsel.repository.RecipeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private Recipe recipe;
    private Comment comment;
    private CommentRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user").role(Role.USER).build();
        recipe = Recipe.builder().id(100L).title("Title").author(user).build();
        comment = Comment.builder()
                .id(10L)
                .text("Great recipe!")
                .user(user)
                .recipe(recipe)
                .createdAt(Instant.now())
                .build();
        request = new CommentRequest("Great recipe!");
    }

    @Test
    @DisplayName("adds comment and returns response")
    void addComment_withValidRequest_returnsCommentResponse() {
        when(recipeRepository.findById(100L)).thenReturn(Optional.of(recipe));
        when(commentMapper.toEntity(request, recipe, user)).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);
        when(commentMapper.toResponse(comment)).thenReturn(CommentResponse.of(comment));

        CommentResponse response = commentService.addComment(100L, request, user);

        assertThat(response.text()).isEqualTo("Great recipe!");
        assertThat(response.userId()).isEqualTo(1L);
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when recipe does not exist")
    void addComment_withNonExistentRecipe_throwsException() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(999L, request, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("returns comments for a recipe")
    void getComments_withExistingRecipe_returnsList() {
        when(recipeRepository.existsById(100L)).thenReturn(true);
        when(commentRepository.findByRecipeIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(comment));
        when(commentMapper.toResponse(comment)).thenReturn(CommentResponse.of(comment));

        List<CommentResponse> responses = commentService.getComments(100L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().text()).isEqualTo("Great recipe!");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when listing comments for non-existent recipe")
    void getComments_withNonExistentRecipe_throwsException() {
        when(recipeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
