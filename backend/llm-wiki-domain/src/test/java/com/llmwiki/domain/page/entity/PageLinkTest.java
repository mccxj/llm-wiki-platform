package com.llmwiki.domain.page.entity;

import com.llmwiki.common.enums.EdgeType;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PageLinkTest {

    @Test
    void shouldCreateWithBuilder() {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        PageLink link = PageLink.builder()
                .id(UUID.randomUUID())
                .sourcePageId(sourceId)
                .targetPageId(targetId)
                .linkType(EdgeType.RELATED_TO)
                .build();

        assertNotNull(link.getId());
        assertEquals(sourceId, link.getSourcePageId());
        assertEquals(targetId, link.getTargetPageId());
        assertEquals(EdgeType.RELATED_TO, link.getLinkType());
    }

    @Test
    void shouldHaveDefaultLinkType() {
        PageLink link = PageLink.builder()
                .sourcePageId(UUID.randomUUID())
                .targetPageId(UUID.randomUUID())
                .build();

        assertEquals(EdgeType.RELATED_TO, link.getLinkType());
    }

    @Test
    void shouldSupportDifferentLinkTypes() {
        PageLink partOf = PageLink.builder()
                .sourcePageId(UUID.randomUUID())
                .targetPageId(UUID.randomUUID())
                .linkType(EdgeType.PART_OF)
                .build();

        assertEquals(EdgeType.PART_OF, partOf.getLinkType());
    }

    @Test
    void shouldSupportNoArgsConstructor() {
        PageLink link = new PageLink();
        link.setSourcePageId(UUID.randomUUID());
        link.setTargetPageId(UUID.randomUUID());
        link.setLinkType(EdgeType.DEPENDS_ON);

        assertNotNull(link.getSourcePageId());
        assertNotNull(link.getTargetPageId());
        assertEquals(EdgeType.DEPENDS_ON, link.getLinkType());
    }
}
