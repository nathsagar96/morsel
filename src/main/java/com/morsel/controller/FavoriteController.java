package com.morsel.controller;

import com.morsel.constants.ApiPaths;
import com.morsel.security.UserPrincipal;
import com.morsel.service.FavoriteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.RECIPE_FAVORITE)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Favorites", description = "Add and remove recipes from favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addFavorite(@PathVariable Long recipeId, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Favorite recipe id={} by user={}", recipeId, principal.user().getId());
        favoriteService.favorite(recipeId, principal.user());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable Long recipeId, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug(
                "Unfavorite recipe id={} by user={}", recipeId, principal.user().getId());
        favoriteService.unfavorite(recipeId, principal.user());
    }
}
