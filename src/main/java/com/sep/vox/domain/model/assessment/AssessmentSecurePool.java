package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

public class AssessmentSecurePool extends BaseModel {
    private UUID assessmentId;
    private AssessmentSecurePoolStatus status;
    private AssessmentSecurePoolReleaseMode releaseMode;
    private Instant embargoUntil; // in automatic mode the pool is released on its own after this time
    private Instant releasedAt;
    private UUID releasedBy;

    public AssessmentSecurePool() {}



    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public AssessmentSecurePoolStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentSecurePoolStatus status) {
        this.status = status;
    }

    public AssessmentSecurePoolReleaseMode getReleaseMode() {
        return releaseMode;
    }

    public void setReleaseMode(AssessmentSecurePoolReleaseMode releaseMode) {
        this.releaseMode = releaseMode;
    }

    public Instant getEmbargoUntil() {
        return embargoUntil;
    }

    public void setEmbargoUntil(Instant embargoUntil) {
        this.embargoUntil = embargoUntil;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public UUID getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(UUID releasedBy) {
        this.releasedBy = releasedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentSecurePool, Builder> {

        private Builder() {
            super(new AssessmentSecurePool());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder assessmentId(UUID assessmentId) {
            target.assessmentId = assessmentId;
            return this;
        }

        public Builder status(AssessmentSecurePoolStatus status) {
            target.status = status;
            return this;
        }

        public Builder releaseMode(AssessmentSecurePoolReleaseMode releaseMode) {
            target.releaseMode = releaseMode;
            return this;
        }

        public Builder embargoUntil(Instant embargoUntil) {
            target.embargoUntil = embargoUntil;
            return this;
        }

        public Builder releasedAt(Instant releasedAt) {
            target.releasedAt = releasedAt;
            return this;
        }

        public Builder releasedBy(UUID releasedBy) {
            target.releasedBy = releasedBy;
            return this;
        }

    }
}
