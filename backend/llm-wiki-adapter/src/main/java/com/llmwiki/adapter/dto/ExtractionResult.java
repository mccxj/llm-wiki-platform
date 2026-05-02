package com.llmwiki.adapter.dto;

import java.util.List;

public class ExtractionResult {
    private List<EntityInfo> entities;
    private List<ConceptInfo> concepts;

    public ExtractionResult() {}

    public List<EntityInfo> getEntities() { return entities; }
    public void setEntities(List<EntityInfo> entities) { this.entities = entities; }
    public List<ConceptInfo> getConcepts() { return concepts; }
    public void setConcepts(List<ConceptInfo> concepts) { this.concepts = concepts; }

    public static class EntityInfo {
        private String name;
        private String type;
        private String description;
        private List<String> relatedEntities;
        private int passCount;
        private double confidence;

        public EntityInfo() {}
        public EntityInfo(String name, String type, String description) {
            this.name = name; this.type = type; this.description = description;
        }
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
        public int getPassCount() { return passCount; }
        public void setPassCount(int passCount) { this.passCount = passCount; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    public static class ConceptInfo {
        private String name;
        private String description;
        private List<String> relatedEntities;
        private int passCount;
        private double confidence;

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
        public int getPassCount() { return passCount; }
        public void setPassCount(int passCount) { this.passCount = passCount; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }
}
