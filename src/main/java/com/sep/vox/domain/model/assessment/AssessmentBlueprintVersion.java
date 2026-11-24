package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentBlueprintVersion extends BaseModel {
    private UUID blueprintId;
    private int version;
    private String code;
    private String description;
    private AssessmentBlueprintVersionStatus status;
    private Integer totalTimeLimitSeconds;
    private Instant effectiveFrom;
    private Instant effectiveTo;

    public AssessmentBlueprintVersion() {}

    public UUID getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(UUID blueprintId) {
        this.blueprintId = blueprintId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssessmentBlueprintVersionStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentBlueprintVersionStatus status) {
        this.status = status;
    }

    public Integer getTotalTimeLimitSeconds() {
        return totalTimeLimitSeconds;
    }

    public void setTotalTimeLimitSeconds(Integer totalTimeLimitSeconds) {
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
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

    public static AssessmentBlueprintVersion draft(
            UUID blueprintId, int version, String code, Instant effectiveFrom, Instant effectiveTo) {
        return builder()
            .blueprintId(blueprintId)
            .version(version)
            .code(code)
            .status(AssessmentBlueprintVersionStatus.DRAFT)
            .effectiveFrom(effectiveFrom)
            .effectiveTo(effectiveTo)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentBlueprintVersion, Builder> {

        private Builder() {
            super(new AssessmentBlueprintVersion());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder blueprintId(UUID blueprintId) {
            target.blueprintId = blueprintId;
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

        public Builder description(String description) {
            target.description = description;
            return this;
        }

        public Builder status(AssessmentBlueprintVersionStatus status) {
            target.status = status;
            return this;
        }

        public Builder totalTimeLimitSeconds(Integer totalTimeLimitSeconds) {
            target.totalTimeLimitSeconds = totalTimeLimitSeconds;
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
    }
}
