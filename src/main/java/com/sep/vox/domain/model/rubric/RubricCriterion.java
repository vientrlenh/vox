package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;

public class RubricCriterion extends BaseModel {
    private UUID rubricVersionId;
    private UUID frameworkCriterionId;
    private String code;
    private String name;
    private String description;
    private RubricCriterionExamples examples;
    private BigDecimal weight;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private int order;
    private boolean isRequired;

    public RubricCriterion() {}

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
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

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public RubricCriterionExamples getExamples() {
        return examples;
    }

    public void setExamples(RubricCriterionExamples examples) {
        this.examples = examples;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    public static RubricCriterion create(
            UUID rubricVersionId, UUID frameworkCriterionId, String code, String name, String description,
            RubricCriterionExamples examples, BigDecimal weight, BigDecimal minScore, BigDecimal maxScore,
            int order, boolean isRequired) {
        return builder()
                .rubricVersionId(rubricVersionId)
                .frameworkCriterionId(frameworkCriterionId)
                .code(code)
                .name(name)
                .description(description)
                .examples(examples)
                .weight(weight)
                .minScore(minScore)
                .maxScore(maxScore)
                .order(order)
                .required(isRequired)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<RubricCriterion, Builder> {

        private Builder() {
            super(new RubricCriterion());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            target.rubricVersionId = rubricVersionId;
            return this;
        }

        public Builder frameworkCriterionId(UUID frameworkCriterionId) {
            target.frameworkCriterionId = frameworkCriterionId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder name(String name) {
            target.name = name;
            return this;
        }

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder examples(RubricCriterionExamples examples) {
            target.examples = examples;
            return this;
        }

        public Builder weight(BigDecimal weight) {
            target.weight = weight;
            return this;
        }

        public Builder minScore(BigDecimal minScore) {
            target.minScore = minScore;
            return this;
        }

        public Builder maxScore(BigDecimal maxScore) {
            target.maxScore = maxScore;
            return this;
        }

        public Builder order(int order) {
            target.order = order;
            return this;
        }

        public Builder required(boolean isRequired) {
            target.isRequired = isRequired;
            return this;
        }
    }
}
