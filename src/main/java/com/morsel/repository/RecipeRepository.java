package com.morsel.repository;

import com.morsel.model.Recipe;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    @EntityGraph(attributePaths = {"author", "ingredients"})
    Optional<Recipe> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"author", "ingredients"})
    Page<Recipe> findByFavoritedBy_Id(Long userId, org.springframework.data.domain.Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "ingredients"})
    Page<Recipe> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "ingredients"})
    Page<Recipe> findAll(@Nullable Specification<Recipe> spec, @NonNull Pageable pageable);
}
