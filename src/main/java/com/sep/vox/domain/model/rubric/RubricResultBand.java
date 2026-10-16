package com.sep.vox.domain.model.rubric;

import java.util.UUID;

import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;

public class RubricResultBand {
    private UUID id;
    private UUID rubricVersionId;
    private String name;
    private String description;
    private PositiveDecimal scoreMin;
    private PositiveDecimal scoreMax;
    private PositiveInteger order;

    public RubricResultBand() {}

    public RubricResultBand(UUID id, UUID rubricVersionId, String name, String description,
            PositiveDecimal scoreMin, PositiveDecimal scoreMax, PositiveInteger order) {
        this.id = id;
        this.rubricVersionId = rubricVersionId;
        this.name = name;
        this.description = description;
        this.scoreMin = scoreMin;
        this.scoreMax = scoreMax;
        this.order = order;
    }

    public RubricResultBand(Builder builder) {
        this.id = builder.id;
        this.rubricVersionId = builder.rubricVersionId;
        this.name = builder.name;
        this.description = builder.description;
        this.scoreMin = builder.scoreMin;
        this.scoreMax = builder.scoreMax;
        this.order = builder.order;
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

    public PositiveDecimal getScoreMin() {
        return scoreMin;
    }

    public void setScoreMin(PositiveDecimal scoreMin) {
        this.scoreMin = scoreMin;
    }

    public PositiveDecimal getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(PositiveDecimal scoreMax) {
        this.scoreMax = scoreMax;
    }

    public PositiveInteger getOrder() {
        return order;
    }

    public void setOrder(PositiveInteger order) {
        this.order = order;
    }

    public static Builder builder() {
        return new RubricResultBand.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID rubricVersionId;
        private String name;
        private String description;
        private PositiveDecimal scoreMin;
        private PositiveDecimal scoreMax;
        private PositiveInteger order;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            this.rubricVersionId = rubricVersionId;
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

        public Builder scoreMin(PositiveDecimal scoreMin) {
            this.scoreMin = scoreMin;
            return this;
        }

        public Builder scoreMax(PositiveDecimal scoreMax) {
            this.scoreMax = scoreMax;
            return this;
        }

        public Builder order(PositiveInteger order) {
            this.order = order;
            return this;
        }

        public RubricResultBand build() {
            return new RubricResultBand(this);
        }
    }
}
