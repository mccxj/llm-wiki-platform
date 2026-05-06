-- Create entity_examples table for few-shot prompting support
CREATE TABLE IF NOT EXISTS entity_examples (
    id UUID PRIMARY KEY DEFAULT UUID(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    example_text TEXT NOT NULL,
    extraction_data TEXT,
    entity_type VARCHAR(50) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index for querying by entity type
CREATE INDEX IF NOT EXISTS idx_entity_examples_entity_type ON entity_examples(entity_type);
CREATE INDEX IF NOT EXISTS idx_entity_examples_deleted ON entity_examples(deleted);
