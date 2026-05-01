package com.llmwiki.domain.page.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageTagId implements Serializable {
    private UUID pageId;
    private String tag;
}
