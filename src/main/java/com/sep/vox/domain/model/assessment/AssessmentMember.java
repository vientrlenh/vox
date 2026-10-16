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

    public AssessmentMember(UUID id, UUID assessmentId, UUID userId, AssessmentMemberRole role, Instant grantedAt,
            UUID grantedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.userId = userId;
        this.role = role;
        this.grantedAt = grantedAt;
        this.grantedBy = grantedBy;
    }

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

    
}
