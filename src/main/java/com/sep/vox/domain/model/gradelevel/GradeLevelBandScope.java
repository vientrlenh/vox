package com.sep.vox.domain.model.gradelevel;

import com.sep.vox.domain.model.BaseModel;
import java.util.UUID;

/**
 * The target band ceiling for a grade level on one version of a competency framework.
 *
 * <p>It stores the band ID, not the band order: the ceiling is one specific BAND ("not above B1"), not
 * a position on the scale ("not above band number 3"). If an administrator inserts a band or renumbers
 * the order inside the framework, storing the ID keeps its meaning, while storing the order would
 * silently point the ceiling at a different band.
 *
 * <p>The invariants "the default band must not be higher than the ceiling band" and "both bands must belong
 * to the same frameworkVersionId" cannot be expressed in the database (both need a join to
 * framework_result_bands), so GradeLevelBandScopeGuardService enforces them.
 */
public class GradeLevelBandScope extends BaseModel {
    private UUID gradeLevelId;
    private UUID frameworkVersionId;
    private UUID defaultTargetBandId;
    private UUID hardMaxBandId;

    public GradeLevelBandScope() {}

    public UUID getGradeLevelId() {
        return gradeLevelId;
    }

    public void setGradeLevelId(UUID gradeLevelId) {
        this.gradeLevelId = gradeLevelId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getDefaultTargetBandId() {
        return defaultTargetBandId;
    }

    public void setDefaultTargetBandId(UUID defaultTargetBandId) {
        this.defaultTargetBandId = defaultTargetBandId;
    }

    public UUID getHardMaxBandId() {
        return hardMaxBandId;
    }

    public void setHardMaxBandId(UUID hardMaxBandId) {
        this.hardMaxBandId = hardMaxBandId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<GradeLevelBandScope, Builder> {

        private Builder() {
            super(new GradeLevelBandScope());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder gradeLevelId(UUID gradeLevelId) {
            target.gradeLevelId = gradeLevelId;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            target.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder defaultTargetBandId(UUID defaultTargetBandId) {
            target.defaultTargetBandId = defaultTargetBandId;
            return this;
        }

        public Builder hardMaxBandId(UUID hardMaxBandId) {
            target.hardMaxBandId = hardMaxBandId;
            return this;
        }
    }
}
