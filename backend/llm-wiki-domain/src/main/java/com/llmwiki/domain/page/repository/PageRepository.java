package com.llmwiki.domain.page.repository;

import com.llmwiki.common.enums.PageStatus;
import com.llmwiki.common.enums.PageType;
import com.llmwiki.domain.page.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<Page> findByStatus(String status, Pageable pageable);
    List<Page> findByPageType(PageType type, Pageable pageable);
    List<Page> findByStatusAndPageType(String status, PageType type, Pageable pageable);

    @Query("SELECT p FROM Page p WHERE p.id NOT IN :connectedIds")
    List<Page> findOrphanPages(@Param("connectedIds") List<UUID> connectedIds);
}
