package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamMember {
    private UUID id;
    private UUID examId;
    private UUID userId;
    private ExamMemberRole role;
    private OffsetDateTime grantedAt;
    private UUID grantedBy; 

    public ExamMember() {}

    public ExamMember(UUID id, UUID examId, UUID userId, ExamMemberRole role, OffsetDateTime grantedAt,
            UUID grantedBy) {
        this.id = id;
        this.examId = examId;
        this.userId = userId;
        this.role = role;
        this.grantedAt = grantedAt;
        this.grantedBy = grantedBy;
    }

    public ExamMember(UUID examId, UUID userId, ExamMemberRole role, OffsetDateTime grantedAt, UUID grantedBy) {
        this.examId = examId;
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

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ExamMemberRole getRole() {
        return role;
    }

    public void setRole(ExamMemberRole role) {
        this.role = role;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(OffsetDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }

    
}
