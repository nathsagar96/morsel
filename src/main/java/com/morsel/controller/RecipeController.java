package com.morsel.controller;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.dto.response.RecipeSummaryResponse;
import com.morsel.exception.BadRequestException;
import com.morsel.security.UserPrincipal;
import com.morsel.service.RecipeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
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

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "title", "averageRating", "createdAt", "updatedAt");

    private final RecipeService recipeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeResponse create(
            @Valid @RequestBody CreateRecipeRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Create recipe request by user: {}", principal.user().getId());
        return recipeService.create(request, principal.user());
    }

    @GetMapping
    public Page<RecipeSummaryResponse> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> ingredients,
            @PageableDefault(size = 20) @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        validateSort(pageable);
        log.debug("Find recipes: keyword={}, ingredients={}, pageable={}", keyword, ingredients, pageable);
        return recipeService.findAll(keyword, ingredients, pageable);
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
    @ResponseStatus(HttpStatus.CREATED)
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

    private void validateSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return;
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Sort field '" + order.getProperty() + "' is not allowed. Allowed: " + ALLOWED_SORT_FIELDS);
            }
        }
    }
}
