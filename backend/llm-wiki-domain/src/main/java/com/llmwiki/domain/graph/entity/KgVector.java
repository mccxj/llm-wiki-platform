package com.llmwiki.domain.graph.entity;

import com.llmwiki.domain.graph.converter.MariaDBVectorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kg_vectors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KgVector {
    @Id
    private UUID nodeId;

    @Version
    private Integer version;

    /**
     * Embedding vector stored as MariaDB 11.8+ native VECTOR(1536).
     * Uses custom {@link MariaDBVectorType} Hibernate UserType which writes
     * vectors via {@link java.sql.PreparedStatement#setObject(int, Object)} —
     * the JDBC driver serialises the float array into MariaDB's native VECTOR
     * format. Reading uses {@link java.sql.ResultSet#getObject(String, Class)}
     * to deserialise back to float[].
     * <p>
     * This replaces the previous {@code @Convert(converter = FloatArrayToJsonConverter.class)}
     * approach which serialised vectors as JSON strings. That approach failed at
     * runtime because standard JDBC {@code setString()} does not invoke MariaDB's
     * {@code VEC_FromText()} SQL function, causing a SQLException on insert/update.
     */
    @Type(MariaDBVectorType.class)
    @Column(nullable = false, columnDefinition = "VECTOR(1536)")
    private float[] vector;

    private String model;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
