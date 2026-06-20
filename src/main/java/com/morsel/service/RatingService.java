package com.morsel.service;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.mapper.RatingMapper;
import com.morsel.model.Rating;
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
    private final RatingMapper ratingMapper;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;

    @Transactional
    public RatingResponse addOrUpdateRating(Long recipeId, RatingRequest request, User user) {
        recipeService.findRecipeOrThrow(recipeId);
        ratingRepository.upsert(request.score(), user.getId(), recipeId);
        recipeRepository.refreshRatingAggregates(recipeId);
        Rating rating = ratingRepository
                .findByUserIdAndRecipeId(user.getId(), recipeId)
                .orElseThrow(() -> new IllegalStateException("Rating not found after upsert for recipe " + recipeId));
        log.info("Rating saved: recipeId={}, userId={}, score={}", recipeId, user.getId(), request.score());
        return ratingMapper.toResponse(rating);
    }
}
