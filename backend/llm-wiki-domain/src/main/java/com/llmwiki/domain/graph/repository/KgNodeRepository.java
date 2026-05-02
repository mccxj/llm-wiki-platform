package com.llmwiki.domain.graph.repository;

import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.entity.KgNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KgNodeRepository extends JpaRepository<KgNode, UUID> {
    Optional<KgNode> findByNameAndNodeType(String name, NodeType nodeType);
    Optional<KgNode> findByNameIgnoreCaseAndNodeType(String name, NodeType nodeType);
    List<KgNode> findByNameContaining(String keyword);
    long countByNodeType(NodeType nodeType);

    @Query("SELECT n FROM KgNode n WHERE n.id NOT IN (SELECT e.sourceNodeId FROM KgEdge e) AND n.id NOT IN (SELECT e.targetNodeId FROM KgEdge e)")
    List<KgNode> findOrphanNodes();
}
