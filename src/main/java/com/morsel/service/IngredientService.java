package com.morsel.service;

import com.morsel.dto.request.IngredientRequest;
import com.morsel.dto.response.IngredientResponse;
import com.morsel.exception.DuplicateResourceException;
import com.morsel.exception.ResourceNotFoundException;
import com.morsel.logging.AuditLogger;
import com.morsel.logging.AuditLogger.Event;
import com.morsel.logging.AuditLogger.Outcome;
import com.morsel.mapper.IngredientMapper;
import com.morsel.model.Ingredient;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Transactional(readOnly = true)
    public Page<IngredientResponse> findAll(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return ingredientRepository.findByNameContainingIgnoreCase(keyword, pageable)
                    .map(ingredientMapper::toResponse);
        }
        return ingredientRepository.findAll(pageable).map(ingredientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public IngredientResponse findById(Long id) {
        Ingredient ingredient = findOrThrow(id);
        return ingredientMapper.toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        Ingredient ingredient = ingredientMapper.toEntity(request);
        try {
            ingredient = ingredientRepository.save(ingredient);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate ingredient name: {}", request.name());
            throw new DuplicateResourceException("Ingredient with name already exists: " + request.name());
        }
        log.info("Ingredient created: id={}, name={}", ingredient.getId(), ingredient.getName());
        return ingredientMapper.toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse update(Long id, IngredientRequest request) {
        Ingredient ingredient = findOrThrow(id);
        ingredient.setName(request.name());
        try {
            ingredient = ingredientRepository.save(ingredient);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate ingredient name on update: {}", request.name());
            throw new DuplicateResourceException("Ingredient with name already exists: " + request.name());
        }
        log.info("Ingredient updated: id={}, name={}", id, ingredient.getName());
        return ingredientMapper.toResponse(ingredient);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new com.morsel.exception.ForbiddenException("Only admins can delete ingredients");
        }
        Ingredient ingredient = findOrThrow(id);
        ingredientRepository.delete(ingredient);
        log.info("Ingredient deleted: id={}, name={}", id, ingredient.getName());
        AuditLogger.log(Event.INGREDIENT_DELETED, currentUser.getId(), Outcome.SUCCESS, "ingredientId=" + id);
    }

    public Ingredient findOrThrow(Long id) {
        return ingredientRepository.findById(id).orElseThrow(() -> {
            log.warn("Ingredient not found: id={}", id);
            return new ResourceNotFoundException("Ingredient not found with id: " + id);
        });
    }
}
