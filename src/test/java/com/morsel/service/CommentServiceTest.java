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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RecipeService recipeService;

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
        when(recipeService.findRecipeOrThrow(100L)).thenReturn(recipe);
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
        when(recipeService.findRecipeOrThrow(999L))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        assertThatThrownBy(() -> commentService.addComment(999L, request, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("returns comments for a recipe")
    void getComments_withExistingRecipe_returnsPage() {
        Pageable pageable = Pageable.unpaged();
        when(recipeRepository.existsById(100L)).thenReturn(true);
        when(commentRepository.findByRecipeIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(commentMapper.toResponse(comment)).thenReturn(CommentResponse.of(comment));

        Page<CommentResponse> responses = commentService.getComments(100L, pageable);

        assertThat(responses).hasSize(1);
        assertThat(responses.getContent().getFirst().text()).isEqualTo("Great recipe!");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when listing comments for non-existent recipe")
    void getComments_withNonExistentRecipe_throwsException() {
        Pageable pageable = Pageable.unpaged();
        when(recipeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(999L, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
