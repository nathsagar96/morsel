package com.morsel.service;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RatingMapper;
import com.morsel.model.Rating;
import com.morsel.model.Recipe;
import com.morsel.model.User;
import com.morsel.repository.RatingRepository;
import com.morsel.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RecipeRepository recipeRepository;
    private final RatingMapper ratingMapper;

    @Transactional
    public RatingResponse addOrUpdateRating(Long recipeId, RatingRequest request, User user) {
        Recipe recipe = findRecipeOrThrow(recipeId);
        ratingRepository.upsert(request.score(), user.getId(), recipeId);
        Rating rating = ratingRepository
                .findByUserIdAndRecipeId(user.getId(), recipeId)
                .orElseThrow(() -> new IllegalStateException("Rating not found after upsert for recipe " + recipeId));
        double average = ratingRepository.findAverageScoreByRecipeId(recipeId).orElse(0.0);
        int count = ratingRepository.findCountByRecipeId(recipeId);
        recipe.setAverageRating(average);
        recipe.setRatingCount(count);
        log.info(
                "Rating saved: recipeId={}, userId={}, score={}, avg={}, count={}",
                recipeId,
                user.getId(),
                request.score(),
                average,
                count);
        return ratingMapper.toResponse(rating);
    }

    private Recipe findRecipeOrThrow(Long id) {
        return recipeRepository.findById(id).orElseThrow(() -> {
            log.warn("Recipe not found: id={}", id);
            return new ResourceNotFoundException("Recipe not found with id: " + id);
        });
    }
}
