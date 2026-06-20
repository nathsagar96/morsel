-- Migration V9: Performance indexes and version column
ALTER TABLE recipes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Enable pg_trgm for trigram-based LIKE search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Composite index for comment listing (recipe_id + created_at DESC)
CREATE INDEX idx_comments_recipe_created ON comments (recipe_id, created_at DESC);

-- Index for recipe listing sorted by creation time
CREATE INDEX idx_recipes_created_at ON recipes (created_at DESC);

-- GIN trigram indexes for keyword search matching LOWER(title/description) LIKE
CREATE INDEX idx_recipes_title_lower_trgm ON recipes USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_recipes_description_lower_trgm ON recipes USING gin (lower(description) gin_trgm_ops);

-- Composite covering index for favorites feed queries
DROP INDEX idx_user_favorite_recipes_user_id;
CREATE INDEX idx_user_favorites_composite ON user_favorite_recipes (user_id, recipe_id);

-- Index on recipe_ingredients join table for ingredient filtering
CREATE INDEX idx_recipe_ingredients_ingredient ON recipe_ingredients (ingredient_id);

-- Composite index for rating aggregate queries
CREATE INDEX idx_ratings_recipe_score ON ratings (recipe_id, score);
