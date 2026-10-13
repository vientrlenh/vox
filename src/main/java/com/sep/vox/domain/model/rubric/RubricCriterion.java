package com.sep.vox.domain.model.rubric;

import java.util.UUID;

import com.sep.vox.domain.valueobject.Code;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;


public class RubricCriterion {
    private UUID id;
    private UUID rubricVersionId;
    private UUID frameworkCriterionId;
    private Code code;
    private String name;
    private String description;
    private RubricCriterionExamples examples;
    private PositiveDecimal weight;
    private PositiveDecimal minScore;
    private PositiveDecimal maxScore;
    private Boolean required;

    protected RubricCriterion() {}

    public RubricCriterion(UUID id, UUID rubricVersionId, UUID frameworkCriterionId, Code code, String name,
            String description, RubricCriterionExamples examples, PositiveDecimal weight, PositiveDecimal minScore, PositiveDecimal maxScore,
            Boolean required) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.frameworkCriterionId = frameworkCriterionId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.examples = examples;
        this.weight = weight;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.required = required;
    }

    public RubricCriterion(Builder builder) {
        this.id = builder.id;
        this.rubricVersionId = builder.rubricVersionId;
        this.frameworkCriterionId = builder.frameworkCriterionId;
        this.code = builder.code;
        this.name = builder.name;
        this.description = builder.description;
        this.examples = builder.examples;
        this.weight = builder.weight;
        this.minScore = builder.minScore;
        this.maxScore = builder.maxScore;
        this.required = builder.required;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public UUID getFrameworkCriterionId() {
        return frameworkCriterionId;
    }

    public void setFrameworkCriterionId(UUID frameworkCriterionId) {
        this.frameworkCriterionId = frameworkCriterionId;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PositiveDecimal getWeight() {
        return weight;
    }

    public void setWeight(PositiveDecimal weight) {
        this.weight = weight;
    }

    public PositiveDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(PositiveDecimal minScore) {
        this.minScore = minScore;
    }

    public PositiveDecimal getMaxScore() {
        return maxScore;
    }

    public RubricCriterionExamples getExamples() {
        return examples;
    }

    public void setExamples(RubricCriterionExamples examples) {
        this.examples = examples;
    }

    public void setMaxScore(PositiveDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public static Builder builder() {
        return new RubricCriterion.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID rubricVersionId;
        private UUID frameworkCriterionId;
        private Code code;
        private String name;
        private String description;
        private RubricCriterionExamples examples;
        private PositiveDecimal weight;
        private PositiveDecimal minScore;
        private PositiveDecimal maxScore;
        private Boolean required;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            this.rubricVersionId = rubricVersionId;
            return this;
        }

        public Builder frameworkCriterionId(UUID frameworkCriterionId) {
            this.frameworkCriterionId = frameworkCriterionId;
            return this;
        }

        public Builder code(Code code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder examples(RubricCriterionExamples examples) {
            this.examples = examples;
            return this;
        }

        public Builder weight(PositiveDecimal weight) {
            this.weight = weight;
            return this;
        }

        public Builder minScore(PositiveDecimal minScore) {
            this.minScore = minScore;
            return this;
        }

        public Builder maxScore(PositiveDecimal maxScore) {
            this.maxScore = maxScore;
            return this;
        }

        public Builder required(Boolean required) {
            this.required = required;
            return this;
        }

        public RubricCriterion build() {
            return new RubricCriterion(this);
        }
    }
}
