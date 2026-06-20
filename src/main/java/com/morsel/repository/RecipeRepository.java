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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long>, JpaSpecificationExecutor<Recipe> {

    @EntityGraph(attributePaths = {"author", "ingredients"})
    Optional<Recipe> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"author"})
    Page<Recipe> findByFavoritedBy_Id(Long userId, org.springframework.data.domain.Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author"})
    Page<Recipe> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"author", "ingredients"})
    Page<Recipe> findAll(@Nullable Specification<Recipe> spec, @NonNull Pageable pageable);

    long countByAuthorId(Long authorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                UPDATE recipes
                SET average_rating = (SELECT COALESCE(AVG(score), 0.0) FROM ratings WHERE recipe_id = :recipeId),
                    rating_count = (SELECT COUNT(*) FROM ratings WHERE recipe_id = :recipeId),
                    version = version + 1
                WHERE id = :recipeId
                """, nativeQuery = true)
    void refreshRatingAggregates(@Param("recipeId") Long recipeId);
}
