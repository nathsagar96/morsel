package com.morsel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.TestcontainersConfiguration;
import com.morsel.model.Ingredient;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import com.morsel.specification.RecipeSpecification;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("RecipeRepository")
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    private User author;
    private Ingredient tomato;
    private Ingredient basil;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        ingredientRepository.deleteAll();

        author = userRepository.save(User.builder()
                .username("chef")
                .email("chef@example.com")
                .password("encoded")
                .role(Role.USER)
                .build());

        tomato = ingredientRepository.save(Ingredient.builder().name("Tomato").build());
        basil = ingredientRepository.save(Ingredient.builder().name("Basil").build());
    }

    @Test
    @DisplayName("finds recipe with author and ingredients when exists")
    void findWithDetailsById_whenExists_returnsRecipeWithAuthorAndIngredients() {
        Recipe recipe = recipeRepository.save(Recipe.builder()
                .title("Pasta")
                .description("A delicious pasta dish")
                .instructions("Boil and serve")
                .author(author)
                .ingredients(List.of(tomato, basil))
                .build());

        Recipe found = recipeRepository.findWithDetailsById(recipe.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Pasta");
        assertThat(found.getAuthor()).isNotNull();
        assertThat(found.getAuthor().getUsername()).isEqualTo("chef");
        assertThat(found.getIngredients())
                .hasSize(2)
                .extracting(Ingredient::getName)
                .containsExactlyInAnyOrder("Tomato", "Basil");
    }

    @Test
    @DisplayName("returns empty when recipe not found")
    void findWithDetailsById_whenNotExists_returnsEmpty() {
        assertThat(recipeRepository.findWithDetailsById(999L)).isEmpty();
    }

    @Test
    @DisplayName("returns paginated recipes with author and ingredients loaded")
    void findAll_withPageable_returnsPageWithEntityGraph() {
        recipeRepository.save(Recipe.builder()
                .title("Recipe A")
                .instructions("Steps")
                .author(author)
                .build());
        recipeRepository.save(Recipe.builder()
                .title("Recipe B")
                .instructions("Steps")
                .author(author)
                .build());

        Page<Recipe> page = recipeRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getAuthor()).isNotNull();
    }

    @Test
    @DisplayName("filters recipes by keyword in title")
    void findAll_withKeywordSpecification_returnsMatchingRecipes() {
        recipeRepository.save(Recipe.builder()
                .title("Chicken Soup")
                .description("Warm and hearty")
                .instructions("Cook slowly")
                .author(author)
                .build());
        recipeRepository.save(Recipe.builder()
                .title("Beef Stew")
                .description("Rich flavor")
                .instructions("Braise")
                .author(author)
                .build());

        Specification<Recipe> spec = RecipeSpecification.withKeyword("chicken");
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Chicken Soup");
    }

    @Test
    @DisplayName("filters recipes by keyword in description")
    void findAll_withKeywordSpecification_matchesDescription() {
        recipeRepository.save(Recipe.builder()
                .title("Pasta")
                .description("A delicious tomato sauce")
                .instructions("Cook")
                .author(author)
                .build());

        Specification<Recipe> spec = RecipeSpecification.withKeyword("tomato");
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("returns empty when keyword has no match")
    void findAll_withKeywordSpecification_whenNoMatch_returnsEmpty() {
        recipeRepository.save(Recipe.builder()
                .title("Salad")
                .instructions("Mix")
                .author(author)
                .build());

        Specification<Recipe> spec = RecipeSpecification.withKeyword("pizza");
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("returns all recipes when specification is match-all")
    void findAll_withMatchAllSpecification_returnsAllRecipes() {
        recipeRepository.save(Recipe.builder()
                .title("Soup")
                .instructions("Boil")
                .author(author)
                .build());
        recipeRepository.save(Recipe.builder()
                .title("Stew")
                .instructions("Braise")
                .author(author)
                .build());

        Page<Recipe> page = recipeRepository.findAll((root, _, cb) -> cb.conjunction(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("filters recipes by ingredient ids")
    void findAll_withIngredientSpecification_returnsMatchingRecipes() {
        recipeRepository.save(Recipe.builder()
                .title("Caprese Salad")
                .instructions("Slice and layer")
                .author(author)
                .ingredients(List.of(tomato, basil))
                .build());

        Specification<Recipe> spec = RecipeSpecification.withIngredients(List.of(tomato.getId()));
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("returns empty when no recipe has the requested ingredient")
    void findAll_withIngredientSpecification_whenNoMatch_returnsEmpty() {
        Ingredient onion =
                ingredientRepository.save(Ingredient.builder().name("Onion").build());

        recipeRepository.save(Recipe.builder()
                .title("Caprese Salad")
                .instructions("Slice")
                .author(author)
                .ingredients(List.of(tomato, basil))
                .build());

        Specification<Recipe> spec = RecipeSpecification.withIngredients(List.of(onion.getId()));
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("escapes LIKE wildcards in keyword search")
    void findAll_withKeywordSpecification_escapesWildcards() {
        recipeRepository.save(Recipe.builder()
                .title("100% Grass Fed Beef")
                .instructions("Grill")
                .author(author)
                .build());

        Specification<Recipe> spec = RecipeSpecification.withKeyword("100%");
        Page<Recipe> page = recipeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }
}
