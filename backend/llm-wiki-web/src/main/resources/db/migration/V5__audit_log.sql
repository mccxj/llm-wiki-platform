CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    operator VARCHAR(200),
    detail TEXT,
    before_value TEXT,
    after_value TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_operator ON audit_log(operator);
CREATE INDEX idx_audit_created_at ON audit_log(created_at);
