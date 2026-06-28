package com.morsel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.TestcontainersConfiguration;
import com.morsel.model.Rating;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("RatingRepository")
class RatingRepositoryTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    private User user;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .username("rater")
                .email("rater@example.com")
                .password("encoded")
                .role(Role.USER)
                .build());

        recipe = recipeRepository.save(Recipe.builder()
                .title("Rated Recipe")
                .instructions("Steps")
                .author(user)
                .build());
    }

    @Test
    @DisplayName("finds rating by user and recipe when exists")
    void findByUserIdAndRecipeId_whenExists_returnsRating() {
        ratingRepository.upsert(4, user.getId(), recipe.getId());

        Optional<Rating> result = ratingRepository.findByUserIdAndRecipeId(user.getId(), recipe.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(4);
        assertThat(result.get().getUser().getUsername()).isEqualTo("rater");
    }

    @Test
    @DisplayName("returns empty when no rating for user and recipe")
    void findByUserIdAndRecipeId_whenNotExists_returnsEmpty() {
        Optional<Rating> result = ratingRepository.findByUserIdAndRecipeId(999L, 999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("inserts a new rating via upsert")
    void upsert_insertsNewRating() {
        ratingRepository.upsert(5, user.getId(), recipe.getId());

        Optional<Rating> result = ratingRepository.findByUserIdAndRecipeId(user.getId(), recipe.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("updates existing rating via upsert")
    void upsert_updatesExistingRating() {
        ratingRepository.upsert(2, user.getId(), recipe.getId());
        ratingRepository.upsert(5, user.getId(), recipe.getId());

        Optional<Rating> result = ratingRepository.findByUserIdAndRecipeId(user.getId(), recipe.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("inserts a new rating via upsert")
    void upsert_insertsNewRating2() {
        ratingRepository.upsert(5, user.getId(), recipe.getId());

        Optional<Rating> result = ratingRepository.findByUserIdAndRecipeId(user.getId(), recipe.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(5);
    }
}
