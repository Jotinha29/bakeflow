package com.bakeflow.audit;

import com.bakeflow.identity.SecuritySupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public AuditService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void record(String event, String entity, UUID id, String description, Object metadata) {
        insert(event, entity, id, description, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(String event, String entity, UUID id, String description, Object metadata) {
        insert(event, entity, id, description, metadata);
    }

    private void insert(String event, String entity, UUID id, String description, Object metadata) {
        String value;
        try {
            value = json.writeValueAsString(metadata);
        } catch (Exception exception) {
            value = "{}";
        }
        UUID actor = SecuritySupport.currentUserId();
        jdbc.update(
                "INSERT INTO audit_events(id,event_type,entity_type,entity_id,actor_type,actor_user_id,description,metadata,occurred_at) VALUES(?,?,?,?,?,?,?,CAST(? AS JSONB),?)",
                UUID.randomUUID(), event, entity, id, actor == null ? "SYSTEM" : "USER", actor,
                description, value, Timestamp.from(Instant.now()));
    }
}
