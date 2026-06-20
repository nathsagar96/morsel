package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RatingMapper;
import com.morsel.model.Rating;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.RatingRepository;
import com.morsel.repository.RecipeRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService")
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingMapper ratingMapper;

    @Mock
    private RecipeService recipeService;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RatingService ratingService;

    private User user;
    private Recipe recipe;
    private Rating rating;
    private RatingRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user").role(Role.USER).build();
        recipe = Recipe.builder().id(100L).title("Title").author(user).build();
        rating = Rating.builder().id(10L).score(4).user(user).recipe(recipe).build();
        request = new RatingRequest(4);
    }

    @Test
    @DisplayName("creates new rating and refreshes recipe aggregates")
    void addOrUpdateRating_newRating_refreshesAggregates() {
        when(recipeService.findRecipeOrThrow(100L)).thenReturn(recipe);
        when(ratingRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(RatingResponse.of(rating));

        RatingResponse response = ratingService.addOrUpdateRating(100L, request, user);

        assertThat(response.score()).isEqualTo(4);
        verify(ratingRepository).upsert(4, 1L, 100L);
        verify(recipeRepository).refreshRatingAggregates(100L);
    }

    @Test
    @DisplayName("updates existing rating and refreshes aggregates")
    void addOrUpdateRating_existingRating_refreshesAggregates() {
        Rating existingRating =
                Rating.builder().id(10L).score(5).user(user).recipe(recipe).build();
        RatingRequest updateRequest = new RatingRequest(5);

        when(recipeService.findRecipeOrThrow(100L)).thenReturn(recipe);
        when(ratingRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.of(existingRating));
        when(ratingMapper.toResponse(existingRating)).thenReturn(RatingResponse.of(existingRating));

        ratingService.addOrUpdateRating(100L, updateRequest, user);

        verify(ratingRepository).upsert(5, 1L, 100L);
        verify(recipeRepository).refreshRatingAggregates(100L);
        verify(ratingMapper).toResponse(existingRating);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when recipe does not exist")
    void addOrUpdateRating_withNonExistentRecipe_throwsException() {
        when(recipeService.findRecipeOrThrow(999L))
                .thenThrow(new ResourceNotFoundException("Recipe not found with id: 999"));

        assertThatThrownBy(() -> ratingService.addOrUpdateRating(999L, request, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
