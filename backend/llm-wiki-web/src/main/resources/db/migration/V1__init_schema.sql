-- LLM Wiki Platform Initial Schema
-- Flyway migration V1

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================
-- Wiki data sources
-- =============================================
CREATE TABLE wiki_sources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    base_url    VARCHAR(512),
    adapter_class VARCHAR(255) NOT NULL,
    config      TEXT,
    enabled     BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================
-- Raw documents (synced from wiki sources)
-- =============================================
CREATE TABLE raw_documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id   VARCHAR(255) NOT NULL,
    source_name VARCHAR(255),
    title       VARCHAR(512),
    content     TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    source_url  TEXT,
    ingested_at TIMESTAMPTZ DEFAULT NOW(),
    last_checked_at TIMESTAMPTZ
);

CREATE INDEX idx_raw_doc_source ON raw_documents(source_id);
CREATE INDEX idx_raw_doc_hash ON raw_documents(content_hash);

-- =============================================
-- Sync logs
-- =============================================
CREATE TABLE sync_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id   UUID NOT NULL REFERENCES wiki_sources(id),
    started_at  TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status      VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    fetched_count INTEGER DEFAULT 0,
    processed_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sync_log_source ON sync_logs(source_id);
CREATE INDEX idx_sync_log_status ON sync_logs(status);

-- =============================================
-- Processing pipeline logs
-- =============================================
CREATE TABLE processing_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_document_id UUID NOT NULL REFERENCES raw_documents(id),
    step        VARCHAR(50) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    detail      TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_proc_log_doc ON processing_log(raw_document_id);
CREATE INDEX idx_proc_log_step ON processing_log(step);

-- =============================================
-- Knowledge graph: nodes
-- =============================================
CREATE TABLE kg_nodes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    node_type   VARCHAR(20) NOT NULL,
    description TEXT,
    page_id     UUID,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_kg_node_name ON kg_nodes(name);
CREATE INDEX idx_kg_node_type ON kg_nodes(node_type);
CREATE UNIQUE INDEX idx_kg_node_unique ON kg_nodes(name, node_type);

-- =============================================
-- Knowledge graph: edges
-- =============================================
CREATE TABLE kg_edges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_node_id UUID NOT NULL REFERENCES kg_nodes(id),
    target_node_id UUID NOT NULL REFERENCES kg_nodes(id),
    edge_type   VARCHAR(20) NOT NULL,
    weight      DECIMAL(5,4) DEFAULT 0.5000,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_kg_edge_source ON kg_edges(source_node_id);
CREATE INDEX idx_kg_edge_target ON kg_edges(target_node_id);
CREATE INDEX idx_kg_edge_type ON kg_edges(edge_type);

-- =============================================
-- Knowledge graph: vectors (pgvector)
-- =============================================
CREATE TABLE kg_vectors (
    node_id     UUID PRIMARY KEY REFERENCES kg_nodes(id),
    vector      vector(1536) NOT NULL,
    model       VARCHAR(100) DEFAULT 'text-embedding-ada-002',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_kg_vector_ivfflat ON kg_vectors USING ivfflat (vector vector_l2_ops) WITH (lists = 100);

-- =============================================
-- Generated pages
-- =============================================
CREATE TABLE pages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(512) NOT NULL,
    slug        VARCHAR(512) NOT NULL UNIQUE,
    content     TEXT,
    page_type   VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    score       DECIMAL(5,2),
    source_doc_id UUID REFERENCES raw_documents(id),
    version     INTEGER DEFAULT 1,
    published_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_page_slug ON pages(slug);
CREATE INDEX idx_page_status ON pages(status);
CREATE INDEX idx_page_type ON pages(page_type);
CREATE INDEX idx_page_source ON pages(source_doc_id);

-- =============================================
-- Page links (cross-references)
-- =============================================
CREATE TABLE page_links (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_page_id UUID NOT NULL REFERENCES pages(id),
    target_page_id UUID NOT NULL REFERENCES pages(id),
    link_type   VARCHAR(20) DEFAULT 'RELATED',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_page_link_source ON page_links(source_page_id);
CREATE INDEX idx_page_link_target ON page_links(target_page_id);

-- =============================================
-- Page tags
-- =============================================
CREATE TABLE page_tags (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id     UUID NOT NULL REFERENCES pages(id),
    tag         VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_page_tag_page ON page_tags(page_id);
CREATE INDEX idx_page_tag_tag ON page_tags(tag);

-- =============================================
-- Approval queue
-- =============================================
CREATE TABLE approval_queue (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id     UUID NOT NULL REFERENCES pages(id),
    action      VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment     TEXT,
    reviewer_id VARCHAR(255),
    reviewed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_approval_status ON approval_queue(status);
CREATE INDEX idx_approval_page ON approval_queue(page_id);

-- =============================================
-- System configuration
-- =============================================
CREATE TABLE system_config (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key  VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(512),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================
-- Seed: default scoring threshold config
-- =============================================
INSERT INTO system_config (config_key, config_value, description) VALUES
('scoring.threshold', '5.0', 'Minimum AI score (0-10) for document processing'),
('scoring.dimensions', 'information_density,entity_richness,knowledge_independence,structure_integrity,timeliness', 'Scoring dimensions'),
('embedding.dimension', '1536', 'Embedding vector dimension'),
('embedding.model', 'text-embedding-ada-002', 'Embedding model name'),
('sync.batch.size', '50', 'Max documents per sync batch'),
('pipeline.max.retries', '3', 'Max retries for failed pipeline steps'),
('scoring.weights', 'information_density:0.3,entity_richness:0.25,knowledge_independence:0.2,structure_integrity:0.15,timeliness:0.1', 'Scoring dimension weights');
