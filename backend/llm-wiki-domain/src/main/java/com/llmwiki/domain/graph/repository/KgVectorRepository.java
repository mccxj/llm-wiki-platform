package com.llmwiki.domain.graph.repository;

import com.llmwiki.domain.graph.entity.KgVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KgVectorRepository extends JpaRepository<KgVector, UUID> {
    Optional<KgVector> findByNodeId(UUID nodeId);
}
