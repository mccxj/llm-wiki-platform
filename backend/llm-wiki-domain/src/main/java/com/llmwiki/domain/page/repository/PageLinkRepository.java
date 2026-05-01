package com.llmwiki.domain.page.repository;

import com.llmwiki.domain.page.entity.PageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PageLinkRepository extends JpaRepository<PageLink, UUID> {
    List<PageLink> findBySourcePageId(UUID sourcePageId);
    List<PageLink> findByTargetPageId(UUID targetPageId);
}
