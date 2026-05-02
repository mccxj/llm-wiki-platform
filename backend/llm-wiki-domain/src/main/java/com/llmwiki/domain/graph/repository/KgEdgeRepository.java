package com.llmwiki.domain.graph.repository;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.domain.graph.entity.KgEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface KgEdgeRepository extends JpaRepository<KgEdge, UUID> {
    List<KgEdge> findBySourceNodeId(UUID sourceNodeId);
    List<KgEdge> findByTargetNodeId(UUID targetNodeId);
    long countByEdgeType(EdgeType edgeType);

    @Query("SELECT DISTINCT n.pageId FROM KgNode n WHERE n.pageId IS NOT NULL AND (EXISTS (SELECT e FROM KgEdge e WHERE e.sourceNodeId = n.id) OR EXISTS (SELECT e FROM KgEdge e WHERE e.targetNodeId = n.id))")
    List<UUID> findConnectedPageIds();
}
