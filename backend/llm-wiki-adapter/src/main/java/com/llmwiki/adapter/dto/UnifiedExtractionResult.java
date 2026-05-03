package com.llmwiki.adapter.dto;

import com.llmwiki.common.enums.AlignmentStatus;

import java.util.List;

public class UnifiedExtractionResult {
    private List<EntityInfo> entities;
    private List<ConceptInfo> concepts;
    private List<RelationInfo> relations;

    public UnifiedExtractionResult() {}

    public List<EntityInfo> getEntities() { return entities; }
    public void setEntities(List<EntityInfo> entities) { this.entities = entities; }
    public List<ConceptInfo> getConcepts() { return concepts; }
    public void setConcepts(List<ConceptInfo> concepts) { this.concepts = concepts; }
    public List<RelationInfo> getRelations() { return relations; }
    public void setRelations(List<RelationInfo> relations) { this.relations = relations; }

    public static class EntityInfo {
        private String name;
        private String type;
        private String description;
        private List<String> relatedEntities;
        private Integer startOffset;
        private Integer endOffset;
        private AlignmentStatus alignmentStatus;
        private Integer extractionIndex;

        public EntityInfo() {}
        public EntityInfo(String name, String type, String description, List<String> relatedEntities) {
            this.name = name; this.type = type; this.description = description; this.relatedEntities = relatedEntities;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getRelatedEntities() { return relatedEntities; }
        public void setRelatedEntities(List<String> relatedEntities) { this.relatedEntities = relatedEntities; }
        public Integer getStartOffset() { return startOffset; }
        public void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }
        public Integer getEndOffset() { return endOffset; }
        public void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }
        public AlignmentStatus getAlignmentStatus() { return alignmentStatus; }
        public void setAlignmentStatus(AlignmentStatus alignmentStatus) { this.alignmentStatus = alignmentStatus; }
        public Integer getExtractionIndex() { return extractionIndex; }
        public void setExtractionIndex(Integer extractionIndex) { this.extractionIndex = extractionIndex; }
    }

    public static class ConceptInfo {
        private String name;
        private String description;
        private List<String> relatedEntities;
        private Integer startOffset;
        private Integer endOffset;
        private AlignmentStatus alignmentStatus;
        private Integer extractionIndex;

        public ConceptInfo() {}
        public ConceptInfo(String name, String description, List<String> relatedEntities) {
            this.name = name; this.description = description; this.relatedEntities = relatedEntities;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getRelatedEntities() { return relatedEntities; }
        public void setRelatedEntities(List<String> relatedEntities) { this.relatedEntities = relatedEntities; }
        public Integer getStartOffset() { return startOffset; }
        public void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }
        public Integer getEndOffset() { return endOffset; }
        public void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }
        public AlignmentStatus getAlignmentStatus() { return alignmentStatus; }
        public void setAlignmentStatus(AlignmentStatus alignmentStatus) { this.alignmentStatus = alignmentStatus; }
        public Integer getExtractionIndex() { return extractionIndex; }
        public void setExtractionIndex(Integer extractionIndex) { this.extractionIndex = extractionIndex; }
    }

    public static class RelationInfo {
        private String sourceEntity;
        private String targetEntity;
        private String relationType;
        private Double confidence;

        public RelationInfo() {}
        public RelationInfo(String sourceEntity, String targetEntity, String relationType, Double confidence) {
            this.sourceEntity = sourceEntity; this.targetEntity = targetEntity;
            this.relationType = relationType; this.confidence = confidence;
        }
        public String getSourceEntity() { return sourceEntity; }
        public void setSourceEntity(String sourceEntity) { this.sourceEntity = sourceEntity; }
        public String getTargetEntity() { return targetEntity; }
        public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public boolean isConfident(double threshold) {
            return confidence != null && confidence >= threshold;
        }

        public boolean hasValidType() {
            return relationType != null && !relationType.isBlank();
        }
    }
}
