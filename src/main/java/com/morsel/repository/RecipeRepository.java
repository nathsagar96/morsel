package com.morsel.repository;

import com.morsel.model.Recipe;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @EntityGraph(attributePaths = {"author", "ingredients"})
    Optional<Recipe> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"author", "ingredients"})
    Page<Recipe> findAll(@NonNull Pageable pageable);
}
