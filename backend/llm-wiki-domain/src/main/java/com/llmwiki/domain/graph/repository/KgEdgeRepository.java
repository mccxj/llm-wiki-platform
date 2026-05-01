package com.llmwiki.domain.graph.repository;

import com.llmwiki.domain.graph.entity.KgEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface KgEdgeRepository extends JpaRepository<KgEdge, UUID> {
    List<KgEdge> findBySourceNodeId(UUID sourceNodeId);
    List<KgEdge> findByTargetNodeId(UUID targetNodeId);
}
