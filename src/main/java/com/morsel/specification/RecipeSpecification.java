package com.morsel.specification;

import com.morsel.model.Ingredient;
import com.morsel.model.Recipe;
import jakarta.persistence.criteria.Join;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class RecipeSpecification {

    private RecipeSpecification() {}

    public static Specification<Recipe> withKeyword(String keyword) {
        return (root, _, cb) -> {
            String escaped =
                    keyword.toLowerCase().replace("!", "!!").replace("_", "!_").replace("%", "!%");
            String pattern = "%" + escaped + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, '!'),
                    cb.like(cb.lower(root.get("description")), pattern, '!'));
        };
    }

    public static Specification<Recipe> withIngredients(List<Long> ingredientIds) {
        return (root, query, _) -> {
            query.distinct(true);
            Join<Recipe, Ingredient> ingredients = root.join("ingredients");
            return ingredients.get("id").in(ingredientIds);
        };
    }
}
