-- 页面来源关联表
CREATE TABLE IF NOT EXISTS page_sources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id         UUID NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    raw_document_id UUID NOT NULL REFERENCES raw_documents(id),
    source_excerpt  TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_page_sources_page ON page_sources(page_id);
CREATE INDEX IF NOT EXISTS idx_page_sources_raw ON page_sources(raw_document_id);
