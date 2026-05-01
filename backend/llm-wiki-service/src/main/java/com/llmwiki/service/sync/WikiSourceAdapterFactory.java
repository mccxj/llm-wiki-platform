package com.llmwiki.service.sync;

import com.llmwiki.adapter.api.WikiSourceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for wiki source adapters.
 * Adapters are registered as Spring beans and looked up by class name.
 */
@Component
@Slf4j
public class WikiSourceAdapterFactory {

    private final Map<String, WikiSourceAdapter> adapters = new ConcurrentHashMap<>();

    public WikiSourceAdapterFactory(List<WikiSourceAdapter> adapterList) {
        // Auto-register all WikiSourceAdapter beans
        for (WikiSourceAdapter adapter : adapterList) {
            String className = adapter.getClass().getSimpleName();
            adapters.put(className, adapter);
            // Also register by the bean class name without "Impl" suffix
            if (className.endsWith("Impl")) {
                adapters.put(className.substring(0, className.length() - 4), adapter);
            }
            log.info("Registered wiki source adapter: {}", className);
        }
    }

    /**
     * Get an adapter by its class name.
     * @param className the adapter class name (as stored in wiki_sources.adapter_class)
     * @return the adapter instance
     * @throws IllegalArgumentException if no adapter found
     */
    public WikiSourceAdapter getAdapter(String className) {
        WikiSourceAdapter adapter = adapters.get(className);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "No wiki source adapter found for class: " + className +
                ". Available adapters: " + adapters.keySet());
        }
        return adapter;
    }
}
