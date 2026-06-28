package com.morsel.service;

import com.morsel.dto.response.RecipeResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RecipeMapper;
import com.morsel.model.User;
import com.morsel.repository.RecipeRepository;
import com.morsel.repository.UserRepository;
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
public class FavoriteService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Transactional
    public void favorite(Long recipeId, User currentUser) {
        recipeRepository
                .findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        int inserted = userRepository.addFavorite(currentUser.getId(), recipeId);
        if (inserted == 0) {
            log.warn("Recipe {} already in favorites for user {}", recipeId, currentUser.getId());
            throw new DuplicateResourceException("Recipe already in favorites");
        }
        log.info("Recipe {} added to favorites for user {}", recipeId, currentUser.getId());
    }

    @Transactional
    public void unfavorite(Long recipeId, User currentUser) {
        int deleted = userRepository.removeFavorite(currentUser.getId(), recipeId);
        if (deleted == 0) {
            log.warn("Recipe {} not in favorites for user {}", recipeId, currentUser.getId());
            throw new ResourceNotFoundException("Recipe not in favorites");
        }
        log.info("Recipe {} removed from favorites for user {}", recipeId, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Page<RecipeResponse> getFavorites(User currentUser, Pageable pageable) {
        log.debug("Fetched favorites for user {} with pageable {}", currentUser.getId(), pageable);
        return recipeRepository
                .findByFavoritedBy_Id(currentUser.getId(), pageable)
                .map(recipeMapper::toResponse);
    }
}
