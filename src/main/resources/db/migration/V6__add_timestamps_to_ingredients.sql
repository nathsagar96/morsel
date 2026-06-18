ALTER TABLE ingredients
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

COMMENT ON COLUMN ingredients.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN ingredients.updated_at IS 'Record last update timestamp';
