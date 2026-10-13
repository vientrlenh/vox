package com.sep.vox.domain.model.rubric;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;
import com.sep.vox.domain.valueobject.PositiveDecimal;
import com.sep.vox.domain.valueobject.PositiveInteger;



public class RubricVersion extends BaseModel {
    private UUID rubricId;
    private PositiveInteger version;
    private String name;
    private String description;
    private RubricStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private PositiveDecimal scoringScaleMin;
    private PositiveDecimal scoringScaleMax;
    private RubricTotalScoreMethod totalScoreMethod;
    private UUID sourceRubricVersionId;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected RubricVersion() {}

    public RubricVersion(UUID rubricId, PositiveInteger version, String name, String description,
            RubricStatus status, Instant effectiveFrom, Instant effectiveTo, PositiveDecimal scoringScaleMin,
            PositiveDecimal scoringScaleMax, RubricTotalScoreMethod totalScoreMethod,
            Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.rubricId = rubricId;
        this.version = version;
        this.name = name;
        this.description = description;
        this.status = status;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.scoringScaleMin = scoringScaleMin;
        this.scoringScaleMax = scoringScaleMax;
        this.totalScoreMethod = totalScoreMethod;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public RubricVersion(Builder builder) {
        super(builder.id, builder.createdAt);
        this.rubricId = builder.rubricId;
        this.version = builder.version;
        this.name = builder.name;
        this.description = builder.description;
        this.status = builder.status;
        this.effectiveFrom = builder.effectiveFrom;
        this.effectiveTo = builder.effectiveTo;
        this.scoringScaleMin = builder.scoringScaleMin;
        this.scoringScaleMax = builder.scoringScaleMax;
        this.totalScoreMethod = builder.totalScoreMethod;
        this.sourceRubricVersionId = builder.sourceRubricVersionId;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
    }

    public UUID getRubricId() {
        return rubricId;
    }

    public void setRubricId(UUID rubricId) {
        this.rubricId = rubricId;
    }

    public PositiveInteger getVersion() {
        return version;
    }

    public void setVersion(PositiveInteger version) {
        this.version = version;
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

    public RubricStatus getStatus() {
        return status;
    }

    public void setStatus(RubricStatus status) {
        this.status = status;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public PositiveDecimal getScoringScaleMin() {
        return scoringScaleMin;
    }

    public void setScoringScaleMin(PositiveDecimal scoringScaleMin) {
        this.scoringScaleMin = scoringScaleMin;
    }

    public PositiveDecimal getScoringScaleMax() {
        return scoringScaleMax;
    }

    public void setScoringScaleMax(PositiveDecimal scoringScaleMax) {
        this.scoringScaleMax = scoringScaleMax;
    }

    public RubricTotalScoreMethod getTotalScoreMethod() {
        return totalScoreMethod;
    }

    public void setTotalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
        this.totalScoreMethod = totalScoreMethod;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getSourceRubricVersionId() {
        return sourceRubricVersionId;
    }

    public void setSourceRubricVersionId(UUID sourceRubricVersionId) {
        this.sourceRubricVersionId = sourceRubricVersionId;
    }

    public static Builder builder() {
        return new RubricVersion.Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID rubricId;
        private PositiveInteger version;
        private String name;
        private String description;
        private RubricStatus status;
        private Instant effectiveFrom;
        private Instant effectiveTo;
        private PositiveDecimal scoringScaleMin;
        private PositiveDecimal scoringScaleMax;
        private RubricTotalScoreMethod totalScoreMethod;
        private UUID sourceRubricVersionId;
        private Instant createdAt;
        private Instant updatedAt;
        private UUID createdBy;
        private UUID updatedBy;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder rubricId(UUID rubricId) {
            this.rubricId = rubricId;
            return this;
        }

        public Builder version(PositiveInteger version) {
            this.version = version;
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

        public Builder status(RubricStatus status) {
            this.status = status;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public Builder scoringScaleMin(PositiveDecimal scoringScaleMin) {
            this.scoringScaleMin = scoringScaleMin;
            return this;
        }

        public Builder scoringScaleMax(PositiveDecimal scoringScaleMax) {
            this.scoringScaleMax = scoringScaleMax;
            return this;
        }

        public Builder totalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
            this.totalScoreMethod = totalScoreMethod;
            return this;
        }

        public Builder sourceRubricVersionId(UUID sourceRubricVersionId) {
            this.sourceRubricVersionId = sourceRubricVersionId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }

        public RubricVersion build() {
            return new RubricVersion(this);
        }
    }
}
