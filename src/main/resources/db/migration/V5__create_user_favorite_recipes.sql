CREATE TABLE user_favorite_recipes (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, recipe_id)
);

CREATE INDEX idx_user_favorite_recipes_user_id ON user_favorite_recipes(user_id);
CREATE INDEX idx_user_favorite_recipes_recipe_id ON user_favorite_recipes(recipe_id);
