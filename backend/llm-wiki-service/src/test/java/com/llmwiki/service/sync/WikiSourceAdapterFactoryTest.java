package com.llmwiki.service.sync;

import com.llmwiki.adapter.api.WikiSourceAdapter;
import com.llmwiki.adapter.dto.RawDocumentDTO;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WikiSourceAdapterFactoryTest {

    @Test
    void shouldRegisterAdapterByClassName() {
        WikiSourceAdapter adapter = new TestAdapter();
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(List.of(adapter));

        WikiSourceAdapter result = factory.getAdapter("TestAdapter");
        assertSame(adapter, result);
    }

    @Test
    void shouldRegisterAdapterWithoutImplSuffix() {
        WikiSourceAdapter adapter = new TestWikiSourceAdapterImpl();
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(List.of(adapter));

        WikiSourceAdapter result = factory.getAdapter("TestWikiSourceAdapter");
        assertSame(adapter, result);
    }

    @Test
    void shouldThrowForUnknownAdapter() {
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(Collections.emptyList());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getAdapter("NonExistentAdapter"));
        assertTrue(ex.getMessage().contains("No wiki source adapter found"));
        assertTrue(ex.getMessage().contains("NonExistentAdapter"));
    }

    @Test
    void shouldThrowWithAvailableAdaptersListed() {
        WikiSourceAdapter adapter = new MyAdapter();
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(List.of(adapter));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getAdapter("UnknownAdapter"));
        assertTrue(ex.getMessage().contains("Available adapters"));
        assertTrue(ex.getMessage().contains("UnknownAdapter"));
    }

    @Test
    void shouldHandleMultipleAdapters() {
        WikiSourceAdapter adapter1 = new AdapterOne();
        WikiSourceAdapter adapter2 = new AdapterTwoImpl();
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(List.of(adapter1, adapter2));

        assertSame(adapter1, factory.getAdapter("AdapterOne"));
        assertSame(adapter2, factory.getAdapter("AdapterTwoImpl"));
        assertSame(adapter2, factory.getAdapter("AdapterTwo"));
    }

    @Test
    void shouldHandleEmptyAdapterList() {
        WikiSourceAdapterFactory factory = new WikiSourceAdapterFactory(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> factory.getAdapter("Any"));
    }

    static class TestAdapter implements WikiSourceAdapter {
        public boolean testConnection() { return true; }
        public List<RawDocumentDTO> fetchChanges(Instant since) { return Collections.emptyList(); }
        public RawDocumentDTO fetchPage(String pageId) { return null; }
    }

    static class TestWikiSourceAdapterImpl implements WikiSourceAdapter {
        public boolean testConnection() { return true; }
        public List<RawDocumentDTO> fetchChanges(Instant since) { return Collections.emptyList(); }
        public RawDocumentDTO fetchPage(String pageId) { return null; }
    }

    static class MyAdapter implements WikiSourceAdapter {
        public boolean testConnection() { return true; }
        public List<RawDocumentDTO> fetchChanges(Instant since) { return Collections.emptyList(); }
        public RawDocumentDTO fetchPage(String pageId) { return null; }
    }

    static class AdapterOne implements WikiSourceAdapter {
        public boolean testConnection() { return true; }
        public List<RawDocumentDTO> fetchChanges(Instant since) { return Collections.emptyList(); }
        public RawDocumentDTO fetchPage(String pageId) { return null; }
    }

    static class AdapterTwoImpl implements WikiSourceAdapter {
        public boolean testConnection() { return true; }
        public List<RawDocumentDTO> fetchChanges(Instant since) { return Collections.emptyList(); }
        public RawDocumentDTO fetchPage(String pageId) { return null; }
    }
}
