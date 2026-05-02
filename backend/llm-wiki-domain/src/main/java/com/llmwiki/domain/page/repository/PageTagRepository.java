package com.llmwiki.domain.page.repository;

import com.llmwiki.domain.page.entity.PageTag;
import com.llmwiki.domain.page.entity.PageTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PageTagRepository extends JpaRepository<PageTag, PageTagId> {
    List<PageTag> findByPageId(UUID pageId);

    List<PageTag> findByTagIgnoreCase(String tag);
}
