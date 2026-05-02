package com.llmwiki.domain.example.repository;

import com.llmwiki.domain.example.entity.EntityExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityExampleRepository extends JpaRepository<EntityExample, UUID> {
    List<EntityExample> findByEntityType(String entityType);

    List<EntityExample> findByEntityTypeAndDeletedFalse(String entityType);

    @Query("SELECT e FROM EntityExample e WHERE e.deleted = false")
    List<EntityExample> findAllActive();
}
