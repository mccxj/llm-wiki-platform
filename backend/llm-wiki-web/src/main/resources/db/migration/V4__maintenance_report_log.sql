CREATE TABLE maintenance_report_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_type VARCHAR(100) NOT NULL,
    result TEXT,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_mrl_task_type ON maintenance_report_log(task_type);
CREATE INDEX idx_mrl_created_at ON maintenance_report_log(created_at);
