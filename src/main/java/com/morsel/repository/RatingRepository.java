package com.morsel.repository;

import com.morsel.model.Rating;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<Rating> findByUserIdAndRecipeId(Long userId, Long recipeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO ratings (score, user_id, recipe_id)
            VALUES (:score, :userId, :recipeId)
            ON CONFLICT (user_id, recipe_id) DO UPDATE SET score = :score
            """, nativeQuery = true)
    void upsert(@Param("score") Integer score, @Param("userId") Long userId, @Param("recipeId") Long recipeId);
}
