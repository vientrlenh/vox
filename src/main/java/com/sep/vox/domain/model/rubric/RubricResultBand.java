package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class RubricResultBand extends BaseModel {
    private UUID rubricVersionId;
    private String code;
    private String name;
    private String description;
    private BigDecimal scoreMin;
    private BigDecimal scoreMax;
    private int order;

    public RubricResultBand() {}

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
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

    public BigDecimal getScoreMin() {
        return scoreMin;
    }

    public void setScoreMin(BigDecimal scoreMin) {
        this.scoreMin = scoreMin;
    }

    public BigDecimal getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(BigDecimal scoreMax) {
        this.scoreMax = scoreMax;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<RubricResultBand, Builder> {

        private Builder() {
            super(new RubricResultBand());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            target.rubricVersionId = rubricVersionId;
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

        public Builder scoreMin(BigDecimal scoreMin) {
            target.scoreMin = scoreMin;
            return this;
        }

        public Builder scoreMax(BigDecimal scoreMax) {
            target.scoreMax = scoreMax;
            return this;
        }

        public Builder order(int order) {
            target.order = order;
            return this;
        }
    }
}
