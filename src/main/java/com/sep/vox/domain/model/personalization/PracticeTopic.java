package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class PracticeTopic {

    private UUID id;
    private String name;
    private String normalizedName;
    private String description;
    private String source;
    private String interestDimension;
    private String curriculumGroup;
    private boolean active;
    private Instant createdAt;
    private UUID sourceQuestionTopicId;

    public PracticeTopic() {
    }

    public PracticeTopic(
            UUID id,
            String name,
            String normalizedName,
            String description,
            String source,
            String interestDimension,
            String curriculumGroup,
            boolean active,
            Instant createdAt,
            UUID sourceQuestionTopicId) {
        this.id = id;
        this.name = name;
        this.normalizedName = normalizedName;
        this.description = description;
        this.source = source;
        this.interestDimension = interestDimension;
        this.curriculumGroup = curriculumGroup;
        this.active = active;
        this.createdAt = createdAt;
        this.sourceQuestionTopicId = sourceQuestionTopicId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInterestDimension() {
        return interestDimension;
    }

    public void setInterestDimension(String interestDimension) {
        this.interestDimension = interestDimension;
    }

    public String getCurriculumGroup() {
        return curriculumGroup;
    }

    public void setCurriculumGroup(String curriculumGroup) {
        this.curriculumGroup = curriculumGroup;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getSourceQuestionTopicId() {
        return sourceQuestionTopicId;
    }

    public void setSourceQuestionTopicId(UUID sourceQuestionTopicId) {
        this.sourceQuestionTopicId = sourceQuestionTopicId;
    }
}
