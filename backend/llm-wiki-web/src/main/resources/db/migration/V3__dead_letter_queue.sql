CREATE TABLE dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT UUID(),
    raw_document_id UUID,
    step VARCHAR(100) NOT NULL,
    error_message TEXT,
    payload TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_dlq_status ON dead_letter_queue(status);
CREATE INDEX idx_dlq_raw_doc ON dead_letter_queue(raw_document_id);
