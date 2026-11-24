package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentMember {
    private UUID id;
    private UUID assessmentId;
    private UUID userId;
    private AssessmentMemberRole role;
    private Instant grantedAt;
    private UUID grantedBy; 

    public AssessmentMember() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AssessmentMemberRole getRole() {
        return role;
    }

    public void setRole(AssessmentMemberRole role) {
        this.role = role;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    public static AssessmentMember granted(
            UUID assessmentId, UUID userId, AssessmentMemberRole role, Instant grantedAt, UUID grantedBy) {
        return builder()
            .assessmentId(assessmentId)
            .userId(userId)
            .role(role)
            .grantedAt(grantedAt)
            .grantedBy(grantedBy)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentMember target = new AssessmentMember();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder assessmentId(UUID assessmentId) {
            target.assessmentId = assessmentId;
            return this;
        }

        public Builder userId(UUID userId) {
            target.userId = userId;
            return this;
        }

        public Builder role(AssessmentMemberRole role) {
            target.role = role;
            return this;
        }

        public Builder grantedAt(Instant grantedAt) {
            target.grantedAt = grantedAt;
            return this;
        }

        public Builder grantedBy(UUID grantedBy) {
            target.grantedBy = grantedBy;
            return this;
        }

        public AssessmentMember build() {
            return target;
        }

    }
}
