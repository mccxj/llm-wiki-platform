-- Update scoring seed data to align with PRD
-- PRD specifies: information_density:0.3, entity_richness:0.25, knowledge_independence:0.2, structure_integrity:0.15, timeliness:0.1
UPDATE system_config SET config_value = 'information_density:0.3,entity_richness:0.25,knowledge_independence:0.2,structure_integrity:0.15,timeliness:0.1',
    description = 'Scoring dimensions (information_density, entity_richness, knowledge_independence, structure_integrity, timeliness)'
WHERE config_key = 'scoring.dimensions';

-- Update threshold to 5.0 (PRD default, 0-10 scale)
UPDATE system_config SET config_value = '5.0',
    description = 'Minimum AI score (0-10) for document processing'
WHERE config_key = 'scoring.threshold';

-- Add per-dimension minimum scores (P1-5)
INSERT INTO system_config (config_key, config_value, description) VALUES
('scoring.min.information_density', '3.0', 'Minimum score for information_density dimension (0-10)'),
('scoring.min.entity_richness', '3.0', 'Minimum score for entity_richness dimension (0-10)'),
('scoring.min.knowledge_independence', '2.0', 'Minimum score for knowledge_independence dimension (0-10)'),
('scoring.min.structure_integrity', '2.0', 'Minimum score for structure_integrity dimension (0-10)'),
('scoring.min.timeliness', '1.0', 'Minimum score for timeliness dimension (0-10)');

-- =============================================
-- Fix page_tags table: use composite primary key
-- =============================================
ALTER TABLE page_tags DROP COLUMN IF EXISTS id;
ALTER TABLE page_tags ADD PRIMARY KEY (page_id, tag);

-- =============================================
-- Enhance approval_queue with entity-level approval support
-- =============================================
ALTER TABLE approval_queue ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50) DEFAULT 'PAGE';
ALTER TABLE approval_queue ADD COLUMN IF NOT EXISTS before_value TEXT;
ALTER TABLE approval_queue ADD COLUMN IF NOT EXISTS after_value TEXT;
ALTER TABLE approval_queue ADD COLUMN IF NOT EXISTS summary TEXT;

-- =============================================
-- Add base_url to wiki_sources if missing
-- =============================================
ALTER TABLE wiki_sources ADD COLUMN IF NOT EXISTS base_url VARCHAR(512);

