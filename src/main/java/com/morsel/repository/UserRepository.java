package com.morsel.repository;

import com.morsel.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findWithRecipesByUsername(String username);

    Optional<User> findWithFavoritesById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                    INSERT INTO user_favorite_recipes (user_id, recipe_id)
                    VALUES (:userId, :recipeId)
                    ON CONFLICT (user_id, recipe_id) DO NOTHING
                    """, nativeQuery = true)
    int addFavorite(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                    DELETE FROM user_favorite_recipes
                    WHERE user_id = :userId AND recipe_id = :recipeId
                    """, nativeQuery = true)
    int removeFavorite(@Param("userId") Long userId, @Param("recipeId") Long recipeId);
}
