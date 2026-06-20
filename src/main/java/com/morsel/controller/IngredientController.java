package com.morsel.controller;

import com.morsel.constants.ApiPaths;
import com.morsel.dto.request.IngredientRequest;
import com.morsel.dto.response.IngredientResponse;
import com.morsel.security.UserPrincipal;
import com.morsel.service.IngredientService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping(ApiPaths.INGREDIENTS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ingredients", description = "Create, read, update, and delete ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    @SecurityRequirements
    public Page<IngredientResponse> findAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        log.debug("List ingredients: keyword={}, pageable={}", keyword, pageable);
        return ingredientService.findAll(keyword, pageable);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    public IngredientResponse findById(@PathVariable Long id) {
        log.debug("Get ingredient by id: {}", id);
        return ingredientService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(
            @Valid @RequestBody IngredientRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Create ingredient by user: {}", principal.user().getId());
        return ingredientService.create(request);
    }

    @PutMapping("/{id}")
    public IngredientResponse update(
            @PathVariable Long id,
            @Valid @RequestBody IngredientRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Update ingredient id={} by user: {}", id, principal.user().getId());
        return ingredientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("Delete ingredient id={} by user: {}", id, principal.user().getId());
        ingredientService.delete(id, principal.user());
    }
}
