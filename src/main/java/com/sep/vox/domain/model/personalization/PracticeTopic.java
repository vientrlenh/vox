package com.sep.vox.domain.model.personalization;

import com.sep.vox.domain.service.personalization.TensePolicy;
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

    /**
     * The tense this topic naturally calls for -- PAST (history, memories) / FUTURE (plans, inventions) /
     * MIXED. Set by topicGenerationGraph when the topic is written, because at that moment the model understands
     * what the topic is about; guessing it back from the topic name later is less faithful.
     *
     * <p>{@code null} counts as MIXED -- see {@code TensePolicy.forSlot}.
     */
    private String temporalAffordance;

    public PracticeTopic() {
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

    public String getTemporalAffordance() {
        return temporalAffordance;
    }

    public void setTemporalAffordance(String temporalAffordance) {
        this.temporalAffordance = temporalAffordance;
    }

    public UUID getSourceQuestionTopicId() {
        return sourceQuestionTopicId;
    }

    public void setSourceQuestionTopicId(UUID sourceQuestionTopicId) {
        this.sourceQuestionTopicId = sourceQuestionTopicId;
    }

    public static PracticeTopic generatedFromName(String source, String name, String normalizedName,
                                                  String interestDimension, String curriculumGroup,
                                                  String temporalAffordance, Instant createdAt) {
        return builder()
                .id(null)
                .name(name)
                .normalizedName(normalizedName)
                .description(name)
                .source(source)
                .interestDimension(interestDimension)
                .curriculumGroup(curriculumGroup == null ? "OUT_OF_CURRICULUM" : curriculumGroup)
                .active(true)
                .createdAt(createdAt)
                .sourceQuestionTopicId(null)
                .temporalAffordance(temporalAffordance == null ? TensePolicy.AFFORDANCE_MIXED : temporalAffordance)
                .build();
    }

    public static PracticeTopic fromAssessmentQuestionTopic(UUID sourceQuestionTopicId, String name,
                                                            String normalizedName, String description,
                                                            String interestDimension, String curriculumGroup,
                                                            Instant createdAt) {
        return builder()
                .name(name)
                .normalizedName(normalizedName)
                .description(description)
                .source("ASSESSMENT_QUESTION_BANK")
                .interestDimension(interestDimension)
                .curriculumGroup(curriculumGroup)
                .active(true)
                .createdAt(createdAt)
                .sourceQuestionTopicId(sourceQuestionTopicId)
                // The topic comes from the school's question bank: nobody assigned a tense, and guessing from the
                // topic name has no basis. MIXED lets the rotating scale spread the tenses itself.
                .temporalAffordance(TensePolicy.AFFORDANCE_MIXED)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final PracticeTopic target = new PracticeTopic();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder normalizedName(String normalizedName) {
            target.normalizedName = normalizedName;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder source(String source) {
            target.source = source;
            return this;
        }

        public Builder interestDimension(String interestDimension) {
            target.interestDimension = interestDimension;
            return this;
        }

        public Builder curriculumGroup(String curriculumGroup) {
            target.curriculumGroup = curriculumGroup;
            return this;
        }

        public Builder active(boolean active) {
            target.active = active;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder sourceQuestionTopicId(UUID sourceQuestionTopicId) {
            target.sourceQuestionTopicId = sourceQuestionTopicId;
            return this;
        }

        public Builder temporalAffordance(String temporalAffordance) {
            target.temporalAffordance = temporalAffordance;
            return this;
        }

        public PracticeTopic build() {
            return target;
        }

    }
}
