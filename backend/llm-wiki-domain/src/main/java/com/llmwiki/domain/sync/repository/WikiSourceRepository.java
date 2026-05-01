package com.llmwiki.domain.sync.repository;

import com.llmwiki.domain.sync.entity.WikiSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WikiSourceRepository extends JpaRepository<WikiSource, UUID> {
    List<WikiSource> findByEnabledTrue();
}
