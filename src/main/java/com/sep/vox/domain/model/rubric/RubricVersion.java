package com.sep.vox.domain.model.rubric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class RubricVersion extends BaseModel {
    private UUID rubricId;
    private int version;
    private String code;
    private String name;
    private String description;
    private RubricStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private BigDecimal scoringScaleMin;
    private BigDecimal scoringScaleMax;
    private RubricTotalScoreMethod totalScoreMethod;
    /**
     * The source version this one was copied from, or null if the owner wrote it themselves.
     *
     * <p>Deliberately NOT in the constructor: adding a parameter there forces every place that builds a RubricVersion
     * to change, the vast majority only to pass null. The copying side calls the setter itself.
     */
    private UUID sourceRubricVersionId;

    public RubricVersion() {}

    public UUID getRubricId() {
        return rubricId;
    }

    public void setRubricId(UUID rubricId) {
        this.rubricId = rubricId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public BigDecimal getScoringScaleMin() {
        return scoringScaleMin;
    }

    public void setScoringScaleMin(BigDecimal scoringScaleMin) {
        this.scoringScaleMin = scoringScaleMin;
    }

    public BigDecimal getScoringScaleMax() {
        return scoringScaleMax;
    }

    public void setScoringScaleMax(BigDecimal scoringScaleMax) {
        this.scoringScaleMax = scoringScaleMax;
    }

    public RubricTotalScoreMethod getTotalScoreMethod() {
        return totalScoreMethod;
    }

    public void setTotalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
        this.totalScoreMethod = totalScoreMethod;
    }

    public UUID getSourceRubricVersionId() {
        return sourceRubricVersionId;
    }

    public void setSourceRubricVersionId(UUID sourceRubricVersionId) {
        this.sourceRubricVersionId = sourceRubricVersionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<RubricVersion, Builder> {

        private Builder() {
            super(new RubricVersion());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder rubricId(UUID rubricId) {
            target.rubricId = rubricId;
            return this;
        }

        public Builder version(int version) {
            target.version = version;
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

        public Builder status(RubricStatus status) {
            target.status = status;
            return this;
        }

        public Builder effectiveFrom(Instant effectiveFrom) {
            target.effectiveFrom = effectiveFrom;
            return this;
        }

        public Builder effectiveTo(Instant effectiveTo) {
            target.effectiveTo = effectiveTo;
            return this;
        }

        public Builder scoringScaleMin(BigDecimal scoringScaleMin) {
            target.scoringScaleMin = scoringScaleMin;
            return this;
        }

        public Builder scoringScaleMax(BigDecimal scoringScaleMax) {
            target.scoringScaleMax = scoringScaleMax;
            return this;
        }

        public Builder totalScoreMethod(RubricTotalScoreMethod totalScoreMethod) {
            target.totalScoreMethod = totalScoreMethod;
            return this;
        }

        public Builder sourceRubricVersionId(UUID sourceRubricVersionId) {
            target.sourceRubricVersionId = sourceRubricVersionId;
            return this;
        }
    }
}
