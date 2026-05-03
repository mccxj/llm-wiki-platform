package com.llmwiki.service.schema;

import com.llmwiki.common.enums.EdgeType;
import com.llmwiki.common.enums.NodeType;
import com.llmwiki.domain.graph.repository.KgEdgeRepository;
import com.llmwiki.domain.graph.repository.KgNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaGeneratorServiceTest {

    @Mock
    private KgNodeRepository kgNodeRepo;

    @Mock
    private KgEdgeRepository kgEdgeRepo;

    @InjectMocks
    private SchemaGeneratorService schemaGeneratorService;

    @Test
    void generateSchema_shouldNotThrow() {
        when(kgNodeRepo.count()).thenReturn(10L);
        when(kgEdgeRepo.count()).thenReturn(20L);
        for (NodeType type : NodeType.values()) {
            when(kgNodeRepo.countByNodeType(type)).thenReturn(2L);
        }
        for (EdgeType type : EdgeType.values()) {
            when(kgEdgeRepo.countByEdgeType(type)).thenReturn(1L);
        }

        assertDoesNotThrow(() -> schemaGeneratorService.generateSchema());

        verify(kgNodeRepo).count();
        verify(kgEdgeRepo).count();
    }

    @Test
    void generateSchema_shouldWorkWithEmptyGraph() {
        when(kgNodeRepo.count()).thenReturn(0L);
        when(kgEdgeRepo.count()).thenReturn(0L);
        for (NodeType type : NodeType.values()) {
            when(kgNodeRepo.countByNodeType(type)).thenReturn(0L);
        }
        for (EdgeType type : EdgeType.values()) {
            when(kgEdgeRepo.countByEdgeType(type)).thenReturn(0L);
        }

        assertDoesNotThrow(() -> schemaGeneratorService.generateSchema());
    }
}
