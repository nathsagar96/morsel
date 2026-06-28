package com.morsel.service;

import com.morsel.event.RatingChangedEvent;
import com.morsel.repository.RecipeRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class RatingAggregateService {

    private final RecipeRepository recipeRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRatingChanged(RatingChangedEvent event) {
        recipeRepository.refreshRatingAggregates(event.recipeId());
        log.debug("Refreshed rating aggregates for recipeId={}", event.recipeId());
    }
}
