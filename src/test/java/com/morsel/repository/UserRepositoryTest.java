package com.morsel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.TestcontainersConfiguration;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import java.util.List;
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
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build());
    }

    @Test
    @DisplayName("finds user by username when exists")
    void findByUsername_whenExists_returnsUser() {
        Optional<User> result = userRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isNotNull();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("returns empty when username not found")
    void findByUsername_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("finds user by email when exists")
    void findByEmail_whenExists_returnsUser() {
        Optional<User> result = userRepository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("returns empty when email not found")
    void findByEmail_whenNotExists_returnsEmpty() {
        Optional<User> result = userRepository.findByEmail("unknown@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("checks username existence returns true when exists")
    void existsByUsername_whenExists_returnsTrue() {
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    @DisplayName("checks username existence returns false when not exists")
    void existsByUsername_whenNotExists_returnsFalse() {
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }

    @Test
    @DisplayName("checks email existence returns true when exists")
    void existsByEmail_whenExists_returnsTrue() {
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("checks email existence returns false when not exists")
    void existsByEmail_whenNotExists_returnsFalse() {
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("finds user with recipes eagerly by username")
    void findWithRecipesByUsername_whenExists_returnsUserWithRecipes() {
        User chef = User.builder()
                .username("chef")
                .email("chef@example.com")
                .password("encoded")
                .role(Role.USER)
                .build();
        Recipe chefSpecial = Recipe.builder()
                .title("Chef Special")
                .instructions("Cook")
                .author(chef)
                .build();
        chef.setRecipes(List.of(chefSpecial));
        userRepository.save(chef);

        Optional<User> result = userRepository.findWithRecipesByUsername("chef");

        assertThat(result).isPresent();
        assertThat(result.get().getRecipes()).hasSize(1);
        assertThat(result.get().getRecipes().get(0).getTitle()).isEqualTo("Chef Special");
    }

    @Test
    @DisplayName("returns empty when username not found")
    void findWithRecipesByUsername_whenNotExists_returnsEmpty() {
        assertThat(userRepository.findWithRecipesByUsername("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("finds user with favorites eagerly by id")
    void findWithFavoritesById_whenExists_returnsUserWithFavorites() {
        User chef = userRepository.save(User.builder()
                .username("chef")
                .email("chef@example.com")
                .password("encoded")
                .role(Role.USER)
                .build());
        Recipe recipe = recipeRepository.save(Recipe.builder()
                .title("Favorite Dish")
                .instructions("Cook")
                .author(chef)
                .build());

        chef.getFavorites().add(recipe);
        userRepository.save(chef);

        Optional<User> result = userRepository.findWithFavoritesById(chef.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getFavorites()).hasSize(1);
        assertThat(result.get().getFavorites().get(0).getTitle()).isEqualTo("Favorite Dish");
    }

    @Test
    @DisplayName("returns empty when id not found")
    void findWithFavoritesById_whenNotExists_returnsEmpty() {
        assertThat(userRepository.findWithFavoritesById(999L)).isEmpty();
    }
}
