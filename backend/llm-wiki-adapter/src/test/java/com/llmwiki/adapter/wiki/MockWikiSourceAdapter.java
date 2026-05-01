package com.llmwiki.adapter.wiki;

import com.llmwiki.adapter.api.WikiSourceAdapter;
import com.llmwiki.adapter.dto.RawDocumentDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock adapter for testing purposes.
 */
@Component
public class MockWikiSourceAdapter implements WikiSourceAdapter {

    private boolean available = true;
    private final List<RawDocumentDTO> documents = new ArrayList<>();

    @Override
    public boolean testConnection() {
        return available;
    }

    @Override
    public List<RawDocumentDTO> fetchChanges(Instant since) {
        return new ArrayList<>(documents);
    }

    @Override
    public RawDocumentDTO fetchPage(String pageId) {
        return documents.stream()
            .filter(d -> d.getSourceId().equals(pageId))
            .findFirst()
            .orElse(null);
    }

    // Test helper methods
    public void addDocument(RawDocumentDTO doc) { documents.add(doc); }
    public void clearDocuments() { documents.clear(); }
    public void setAvailable(boolean available) { this.available = available; }
}
