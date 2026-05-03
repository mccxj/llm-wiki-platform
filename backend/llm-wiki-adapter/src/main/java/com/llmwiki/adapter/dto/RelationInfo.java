package com.llmwiki.adapter.dto;

import java.io.Serializable;

/**
 * Structured relation between two entities with directionality and confidence.
 * E-6: Structured relation types with directionality and confidence scores.
 */
public class RelationInfo implements Serializable {
    private String sourceEntity;
    private String targetEntity;
    private String relationType;
    private Double confidence;

    public RelationInfo() {}

    public RelationInfo(String sourceEntity, String targetEntity, String relationType, Double confidence) {
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.relationType = relationType;
        this.confidence = confidence;
    }

    public String getSourceEntity() { return sourceEntity; }
    public void setSourceEntity(String sourceEntity) { this.sourceEntity = sourceEntity; }
    public String getTargetEntity() { return targetEntity; }
    public void setTargetEntity(String targetEntity) { this.targetEntity = targetEntity; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    /** Returns true if confidence is above the given threshold. */
    public boolean isConfident(double threshold) {
        return confidence != null && confidence >= threshold;
    }

    /** Returns true if this relation has a valid structured type. */
    public boolean hasValidType() {
        return relationType != null && !relationType.isBlank();
    }
}
