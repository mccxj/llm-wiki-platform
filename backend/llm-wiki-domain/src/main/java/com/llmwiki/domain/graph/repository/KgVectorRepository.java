package com.llmwiki.domain.graph.repository;

import com.llmwiki.domain.graph.entity.KgVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KgVectorRepository extends JpaRepository<KgVector, UUID> {
    Optional<KgVector> findByNodeId(UUID nodeId);

    /**
     * E-7: Find all vectors excluding a specific node.
     * Similarity filtering happens in the service layer (SemanticDedupService)
     * to avoid database-specific vector operators that break H2 compatibility.
     */
    List<KgVector> findByNodeIdNot(UUID excludeId);
}
