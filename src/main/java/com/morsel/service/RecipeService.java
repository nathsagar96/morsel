package com.morsel.service;

import com.morsel.dto.request.CreateRecipeRequest;
import com.morsel.dto.request.UpdateRecipeRequest;
import com.morsel.dto.response.RecipeResponse;
import com.morsel.dto.response.RecipeSummaryResponse;
import com.morsel.exception.ForbiddenException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.mapper.RecipeMapper;
import com.morsel.model.Ingredient;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.IngredientRepository;
import com.morsel.repository.RecipeRepository;
import com.morsel.specification.RecipeSpecification;
import com.morsel.storage.FileStorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeMapper recipeMapper;
    private final FileStorageService fileStorageService;

    @Transactional
    public RecipeResponse create(CreateRecipeRequest request, User author) {
        List<Ingredient> ingredients = lookupIngredients(request.ingredientIds());
        Recipe recipe = recipeMapper.toEntity(request, author, ingredients);
        recipe = recipeRepository.save(recipe);
        log.info("Recipe created: id={}, title={}, author={}", recipe.getId(), recipe.getTitle(), author.getUsername());
        return recipeMapper.toResponse(recipe);
    }

    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> findAll(Pageable pageable) {
        return findAll(null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> findAll(String keyword, List<Long> ingredientIds, Pageable pageable) {
        Specification<Recipe> spec = (_, _, cb) -> cb.conjunction();
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(RecipeSpecification.withKeyword(keyword));
        }
        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            spec = spec.and(RecipeSpecification.withIngredients(ingredientIds));
        }
        return recipeRepository.findAll(spec, pageable).map(recipeMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public RecipeResponse findById(Long id) {
        Recipe recipe = findRecipeOrThrow(id);
        return recipeMapper.toResponse(recipe);
    }

    @Transactional
    public RecipeResponse update(Long id, UpdateRecipeRequest request, User currentUser) {
        Recipe recipe = findRecipeOrThrow(id);
        checkOwnership(recipe, currentUser);
        List<Ingredient> ingredients = lookupIngredients(request.ingredientIds());
        recipeMapper.updateEntity(recipe, request, ingredients);
        log.info("Recipe updated: id={}", recipe.getId());
        return recipeMapper.toResponse(recipe);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Recipe recipe = findRecipeOrThrow(id);
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only admins can delete recipes");
        }
        recipeRepository.delete(recipe);
        log.info("Recipe deleted: id={} by admin={}", id, currentUser.getUsername());
    }

    @Transactional
    public RecipeResponse uploadImage(Long id, MultipartFile file, User currentUser) {
        Recipe recipe = findRecipeOrThrow(id);
        checkOwnership(recipe, currentUser);
        String imageUrl = fileStorageService.store(file);
        recipe.setImageUrl(imageUrl);
        log.info("Image uploaded for recipe id={}, url={}", id, imageUrl);
        return recipeMapper.toResponse(recipe);
    }

    public Recipe findRecipeOrThrow(Long id) {
        return recipeRepository.findWithDetailsById(id).orElseThrow(() -> {
            log.warn("Recipe not found: id={}", id);
            return new ResourceNotFoundException("Recipe not found with id: " + id);
        });
    }

    private void checkOwnership(Recipe recipe, User currentUser) {
        if (!recipe.getAuthor().getId().equals(currentUser.getId())) {
            log.warn(
                    "Forbidden: user={} attempted to modify recipe={} owned by user={}",
                    currentUser.getId(),
                    recipe.getId(),
                    recipe.getAuthor().getId());
            throw new ForbiddenException("You do not own this recipe");
        }
    }

    private List<Ingredient> lookupIngredients(List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return List.of();
        }
        List<Ingredient> found = ingredientRepository.findAllById(ingredientIds);
        if (found.size() != ingredientIds.size()) {
            List<Long> foundIds = found.stream().map(Ingredient::getId).toList();
            List<Long> missing = new ArrayList<>(ingredientIds);
            missing.removeAll(foundIds);
            log.warn("Ingredient IDs not found: {}", missing);
            throw new ResourceNotFoundException("Ingredients not found: " + missing);
        }
        return found;
    }
}
