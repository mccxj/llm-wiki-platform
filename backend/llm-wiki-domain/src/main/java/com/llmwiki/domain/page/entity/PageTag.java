package com.llmwiki.domain.page.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "page_tags")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(PageTagId.class)
public class PageTag {
    @Id
    private UUID pageId;

    @Id
    private String tag;
}
