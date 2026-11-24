package com.sep.vox.domain.model.assessment;

import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentPaper extends BaseModel {
    private UUID assessmentId;
    private UUID blueprintVersionId;
    private String code;
    private int variant;
    private AssessmentPaperStatus status;
    private Integer timeDurationSeconds;

    public AssessmentPaper() {}

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getBlueprintVersionId() {
        return blueprintVersionId;
    }

    public void setBlueprintVersionId(UUID blueprintVersionId) {
        this.blueprintVersionId = blueprintVersionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public AssessmentPaperStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentPaperStatus status) {
        this.status = status;
    }

    public Integer getTimeDurationSeconds() {
        return timeDurationSeconds;
    }

    public void setTimeDurationSeconds(Integer timeDurationSeconds) {
        this.timeDurationSeconds = timeDurationSeconds;
    }

    public static AssessmentPaper draft(UUID assessmentId, UUID blueprintVersionId, String code, int variant) {
        return builder()
            .assessmentId(assessmentId)
            .blueprintVersionId(blueprintVersionId)
            .code(code)
            .variant(variant)
            .status(AssessmentPaperStatus.DRAFT)
            .timeDurationSeconds(0)
            .build();
    }

    public static AssessmentPaper draftWithoutBlueprint(UUID assessmentId, String code, int variant) {
        return draft(assessmentId, null, code, variant);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentPaper, Builder> {

        private Builder() {
            super(new AssessmentPaper());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder assessmentId(UUID assessmentId) {
            target.assessmentId = assessmentId;
            return this;
        }

        public Builder blueprintVersionId(UUID blueprintVersionId) {
            target.blueprintVersionId = blueprintVersionId;
            return this;
        }

        public Builder code(String code) {
            target.code = code;
            return this;
        }

        public Builder variant(int variant) {
            target.variant = variant;
            return this;
        }

        public Builder status(AssessmentPaperStatus status) {
            target.status = status;
            return this;
        }

        public Builder timeDurationSeconds(Integer timeDurationSeconds) {
            target.timeDurationSeconds = timeDurationSeconds;
            return this;
        }
    }
}
