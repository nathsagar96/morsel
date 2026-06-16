-- Migration V1: Create domain tables
-- Table: users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Table: recipes
CREATE TABLE recipes (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT NOT NULL,
    image_url VARCHAR(255),
    author_id BIGINT NOT NULL,
    CONSTRAINT fk_recipes_author FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Table: ingredients
CREATE TABLE ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Table: recipe_ingredients (join table for ManyToMany)
CREATE TABLE recipe_ingredients (
    recipe_id BIGINT NOT NULL REFERENCES recipes(id),
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    PRIMARY KEY (recipe_id, ingredient_id)
);

-- Table: comments
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_comments_recipe FOREIGN KEY (recipe_id) REFERENCES recipes(id)
);

-- Table: ratings
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    score INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ratings_recipe FOREIGN KEY (recipe_id) REFERENCES recipes(id)
);

-- Indexes
CREATE INDEX idx_recipes_author_id ON recipes(author_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_recipe_id ON comments(recipe_id);
CREATE INDEX idx_ratings_user_id ON ratings(user_id);
CREATE INDEX idx_ratings_recipe_id ON ratings(recipe_id);

COMMENT ON COLUMN comments.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN comments.updated_at IS 'Record last update timestamp';