package com.morsel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.morsel.dto.response.RecipeResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RecipeMapper;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService")
class FavoriteServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeMapper recipeMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Recipe recipe;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        user = User.builder().id(1L).username("testuser").role(Role.USER).build();
        recipe = Recipe.builder().id(100L).title("Test Recipe").author(user).build();
    }

    @Test
    @DisplayName("adds recipe to favorites")
    void favorite_withValidIds_addsToFavorites() {
        when(recipeRepository.findById(100L)).thenReturn(Optional.of(recipe));
        when(userRepository.addFavorite(1L, 100L)).thenReturn(1);

        favoriteService.favorite(100L, user);

        verify(userRepository).addFavorite(1L, 100L);
    }

    @Test
    @DisplayName("throws DuplicateResourceException when already favorited")
    void favorite_withAlreadyFavorited_throwsException() {
        when(recipeRepository.findById(100L)).thenReturn(Optional.of(recipe));
        when(userRepository.addFavorite(1L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> favoriteService.favorite(100L, user))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in favorites");
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when recipe not found")
    void favorite_withNonExistentRecipe_throwsException() {
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.favorite(999L, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipe not found");
    }

    @Test
    @DisplayName("removes recipe from favorites")
    void unfavorite_withExistingFavorite_removesFromFavorites() {
        when(userRepository.removeFavorite(1L, 100L)).thenReturn(1);

        favoriteService.unfavorite(100L, user);

        verify(userRepository).removeFavorite(1L, 100L);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when unfavoriting non-favorited recipe")
    void unfavorite_withNonFavorited_throwsException() {
        when(userRepository.removeFavorite(1L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> favoriteService.unfavorite(100L, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not in favorites");
    }

    @Test
    @DisplayName("returns page of favorite recipes")
    void getFavorites_withFavorites_returnsRecipeResponses() {
        var recipePage = new org.springframework.data.domain.PageImpl<>(List.of(recipe));
        when(recipeRepository.findByFavoritedBy_Id(1L, Pageable.unpaged())).thenReturn(recipePage);
        RecipeResponse response =
                new RecipeResponse(100L, "Test Recipe", null, null, null, 1L, "testuser", List.of(), 0.0, 0, now, now);
        when(recipeMapper.toResponse(recipe)).thenReturn(response);

        Page<RecipeResponse> results = favoriteService.getFavorites(user, Pageable.unpaged());

        assertThat(results).hasSize(1);
        assertThat(results.getContent().getFirst().id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("returns empty page when no favorites")
    void getFavorites_withNoFavorites_returnsEmptyList() {
        when(recipeRepository.findByFavoritedBy_Id(1L, Pageable.unpaged()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        Page<RecipeResponse> results = favoriteService.getFavorites(user, Pageable.unpaged());

        assertThat(results).isEmpty();
        verify(recipeMapper, never()).toResponse(any());
    }
}
