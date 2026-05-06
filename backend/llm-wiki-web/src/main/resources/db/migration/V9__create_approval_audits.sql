-- Approval audit trail
-- Creates the approval_audits table referenced by ApprovalAudit entity

CREATE TABLE approval_audits (
    id          UUID PRIMARY KEY DEFAULT UUID(),
    approval_id UUID NOT NULL,
    action      VARCHAR(100) NOT NULL,
    reviewer_id VARCHAR(255) NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_approval_audit_approval ON approval_audits(approval_id);
CREATE INDEX idx_approval_audit_reviewer ON approval_audits(reviewer_id);
CREATE INDEX idx_approval_audit_created_at ON approval_audits(created_at);
