-- Migration V4: Add rating aggregate columns to recipes and unique constraint on ratings
ALTER TABLE recipes
    ADD COLUMN average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    ADD COLUMN rating_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE ratings
    ADD CONSTRAINT uq_ratings_user_recipe UNIQUE (user_id, recipe_id);
