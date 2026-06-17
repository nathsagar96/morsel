package com.morsel.controller;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@Slf4j
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(
            @Valid @RequestBody CreateRecipeRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Create recipe request by user: {}", principal.user().getId());
        return recipeService.create(request, principal.user());
    }

    @GetMapping
    public Page<RecipeResponse> findAll(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("Find all recipes with pageable: {}", pageable);
        return recipeService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public RecipeResponse findById(@PathVariable Long id) {
        log.debug("Find recipe by id: {}", id);
        return recipeService.findById(id);
    }

    @PutMapping("/{id}")
    public RecipeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecipeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Update recipe id={} by user: {}", id, principal.user().getId());
        return recipeService.update(id, request, principal.user());
    }

    @PostMapping("/{id}/image")
    public RecipeResponse uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug(
                "Upload image for recipe id={} by user: {}",
                id,
                principal.user().getId());
        return recipeService.uploadImage(id, file, principal.user());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Delete recipe id={} by user: {}", id, principal.user().getId());
        recipeService.delete(id, principal.user());
    }
}
