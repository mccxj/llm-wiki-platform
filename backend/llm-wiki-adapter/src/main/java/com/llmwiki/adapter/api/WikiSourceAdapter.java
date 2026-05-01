package com.llmwiki.adapter.api;

import com.llmwiki.adapter.dto.RawDocumentDTO;
import java.time.Instant;
import java.util.List;

/**
 * Abstract interface for wiki data source adapters.
 * Implementations should handle specific wiki platforms.
 */
public interface WikiSourceAdapter {

    /**
     * Test the connection to the wiki source.
     */
    boolean testConnection();

    /**
     * Fetch documents changed since the given timestamp.
     */
    List<RawDocumentDTO> fetchChanges(Instant since);

    /**
     * Fetch a single page by ID.
     */
    RawDocumentDTO fetchPage(String pageId);
}
