CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    actor_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    description VARCHAR(300) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at DESC);
CREATE INDEX idx_audit_events_event_type ON audit_events (event_type);
CREATE INDEX idx_audit_events_entity ON audit_events (entity_type, entity_id);
