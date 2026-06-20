package com.morsel.repository;

import com.morsel.model.Ingredient;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Optional<Ingredient> findByName(String name);

    Page<Ingredient> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
