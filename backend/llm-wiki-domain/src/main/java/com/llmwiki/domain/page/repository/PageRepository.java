package com.llmwiki.domain.page.repository;

import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import com.llmwiki.domain.page.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {
    Optional<Page> findBySlug(String slug);
    List<Page> findByStatus(String status);
    List<Page> findByPageType(PageType type);
    List<Page> findByStatusOrderByCreatedAtDesc(PageStatus status, Pageable pageable);
}
