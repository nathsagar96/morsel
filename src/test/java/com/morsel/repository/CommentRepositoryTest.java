package com.morsel.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.morsel.TestcontainersConfiguration;
import com.morsel.model.Comment;
import com.morsel.model.Recipe;
import com.morsel.model.Role;
import com.morsel.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("CommentRepository")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    private User user;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .username("commenter")
                .email("commenter@example.com")
                .password("encoded")
                .role(Role.USER)
                .build());

        recipe = recipeRepository.save(Recipe.builder()
                .title("Test Recipe")
                .instructions("Steps")
                .author(user)
                .build());

        commentRepository.save(
                Comment.builder().text("Old comment").user(user).recipe(recipe).build());
        commentRepository.save(
                Comment.builder().text("New comment").user(user).recipe(recipe).build());
    }

    @Test
    @DisplayName("returns comments for recipe ordered by newest first")
    void findByRecipeIdOrderByCreatedAtDesc_returnsCommentsOrderedByNewest() {
        Page<Comment> page =
                commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipe.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("New comment");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Old comment");
    }

    @Test
    @DisplayName("returns empty page when recipe has no comments")
    void findByRecipeIdOrderByCreatedAtDesc_whenNoComments_returnsEmpty() {
        Page<Comment> page = commentRepository.findByRecipeIdOrderByCreatedAtDesc(999L, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("loads user and recipe eagerly via entity graph")
    void findByRecipeIdOrderByCreatedAtDesc_loadsUserAndRecipeEagerly() {
        Page<Comment> page =
                commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipe.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo("commenter");
        assertThat(page.getContent().get(0).getRecipe().getTitle()).isEqualTo("Test Recipe");
    }

    @Test
    @DisplayName("supports pagination")
    void findByRecipeIdOrderByCreatedAtDesc_supportsPagination() {
        Page<Comment> first =
                commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipe.getId(), PageRequest.of(0, 1));

        assertThat(first.getContent()).hasSize(1);
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
    }
}
