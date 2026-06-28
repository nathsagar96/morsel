package com.morsel.service;

import com.morsel.dto.request.RatingRequest;
import com.morsel.dto.response.RatingResponse;
import com.morsel.event.RatingChangedEvent;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RatingMapper;
import com.morsel.model.Rating;
import com.morsel.model.User;
import com.morsel.repository.RatingRepository;
import com.morsel.repository.RecipeRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;
    private final RecipeRepository recipeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RatingResponse addOrUpdateRating(Long recipeId, RatingRequest request, User user) {
        recipeRepository
                .findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found with id: " + recipeId));
        ratingRepository.upsert(request.score(), user.getId(), recipeId);
        eventPublisher.publishEvent(new RatingChangedEvent(recipeId));
        Rating rating = ratingRepository
                .findByUserIdAndRecipeId(user.getId(), recipeId)
                .orElseThrow(() -> new IllegalStateException("Rating not found after upsert for recipe " + recipeId));
        log.info("Rating saved: recipeId={}, userId={}, score={}", recipeId, user.getId(), request.score());
        return ratingMapper.toResponse(rating);
    }
}
