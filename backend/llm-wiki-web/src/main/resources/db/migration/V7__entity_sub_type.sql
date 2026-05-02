-- P0-1: Persist entity sub-type (PERSON/ORG/TECH/TOOL/OTHER) to kg_nodes
ALTER TABLE kg_nodes ADD COLUMN IF NOT EXISTS entity_sub_type VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_kg_node_sub_type ON kg_nodes(entity_sub_type);
