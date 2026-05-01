package com.llmwiki.domain.sync.repository;

import com.llmwiki.domain.sync.entity.RawDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawDocumentRepository extends JpaRepository<RawDocument, UUID> {
    Optional<RawDocument> findBySourceIdAndSourceName(String sourceId, String sourceName);
    Optional<RawDocument> findBySourceId(String sourceId);
}
