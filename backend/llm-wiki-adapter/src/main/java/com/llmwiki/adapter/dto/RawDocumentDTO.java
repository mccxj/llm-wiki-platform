package com.llmwiki.adapter.dto;

import java.time.Instant;

public class RawDocumentDTO {
    private String sourceId;
    private String title;
    private String content;
    private String sourceUrl;
    private Instant lastModified;
    private String format;

    public RawDocumentDTO() {}

    public RawDocumentDTO(String sourceId, String title, String content, String sourceUrl, Instant lastModified) {
        this.sourceId = sourceId;
        this.title = title;
        this.content = content;
        this.sourceUrl = sourceUrl;
        this.lastModified = lastModified;
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
