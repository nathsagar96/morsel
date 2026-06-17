-- Migration V3: Add timestamps to recipes table
ALTER TABLE recipes
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();
