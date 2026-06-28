package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.event.RatingChangedEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService")
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingMapper ratingMapper;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
    @DisplayName("creates new rating and publishes event")
    void addOrUpdateRating_newRating_publishesEvent() {
        when(recipeRepository.findById(100L)).thenReturn(Optional.of(recipe));
        when(ratingRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.of(rating));
        when(ratingMapper.toResponse(rating)).thenReturn(RatingResponse.of(rating));

        RatingResponse response = ratingService.addOrUpdateRating(100L, request, user);

        assertThat(response.score()).isEqualTo(4);
        verify(ratingRepository).upsert(4, 1L, 100L);

        ArgumentCaptor<RatingChangedEvent> captor = ArgumentCaptor.forClass(RatingChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipeId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("updates existing rating and publishes event")
    void addOrUpdateRating_existingRating_publishesEvent() {
        Rating existingRating =
                Rating.builder().id(10L).score(5).user(user).recipe(recipe).build();
        RatingRequest updateRequest = new RatingRequest(5);

        when(recipeRepository.findById(100L)).thenReturn(Optional.of(recipe));
        when(ratingRepository.findByUserIdAndRecipeId(1L, 100L)).thenReturn(Optional.of(existingRating));
        when(ratingMapper.toResponse(existingRating)).thenReturn(RatingResponse.of(existingRating));

        ratingService.addOrUpdateRating(100L, updateRequest, user);

        verify(ratingRepository).upsert(5, 1L, 100L);
        verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
        verify(ratingMapper).toResponse(existingRating);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when recipe does not exist")
    void addOrUpdateRating_withNonExistentRecipe_throwsException() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.addOrUpdateRating(999L, request, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
