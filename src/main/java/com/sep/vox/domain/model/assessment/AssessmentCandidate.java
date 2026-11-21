package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentCandidate {
    private UUID id;
    private UUID assessmentId;
    private UUID studentId;
    private UUID assignedPaperId;
    private UUID scheduleId;
    private AssessmentCandidateStatus status;
    private Instant assignedAt;
    private Instant updatedAt;
    private Instant blockedAt;
    private UUID assignedBy;
    private UUID updatedBy;

    public AssessmentCandidate() {}

    /**
     * Tạo mới một thí sinh cho kỳ thi ở trạng thái ASSIGNED (id do DB sinh), chưa gán ca và chưa gán đề.
     */
    public static AssessmentCandidate createFresh(UUID assessmentId, UUID studentId, UUID createdBy, Instant now) {
        return AssessmentCandidate.builder()
                .assessmentId(assessmentId)
                .studentId(studentId)
                .assignedPaperId(null)
                .scheduleId(null)
                .status(AssessmentCandidateStatus.ASSIGNED)
                .assignedAt(now)
                .updatedAt(now)
                .blockedAt(null)
                .assignedBy(createdBy)
                .updatedBy(createdBy)
                .build();
    }

    /**
     * Gán thí sinh vào một ca thi (= một phòng). Guard nghiệp vụ (trạng thái ca, sức chứa) để ở use case.
     */
    public void assignToSchedule(UUID scheduleId, Instant now, UUID updatedBy) {
        this.scheduleId = scheduleId;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    /**
     * Bỏ gán thí sinh khỏi ca thi hiện tại.
     */
    public void unassignFromSchedule(Instant now, UUID updatedBy) {
        this.scheduleId = null;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    /**
     * Gán mã đề (đã khoá) cho thí sinh. Guard "đề đã LOCKED" để ở use case.
     */
    public void assignPaper(UUID paperId, Instant now, UUID updatedBy) {
        this.assignedPaperId = paperId;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
    }

    /**
     * Bỏ gán mã đề khỏi thí sinh. Dùng khi mã đề bị xoá: thí sinh quay về "chưa phân đề" thay vì
     * giữ con trỏ tới một dòng không còn tồn tại.
     */
    public void unassignPaper(Instant now, UUID updatedBy) {
        this.assignedPaperId = null;
        this.updatedAt = now;
        this.updatedBy = updatedBy;
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

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getAssignedPaperId() {
        return assignedPaperId;
    }

    public void setAssignedPaperId(UUID assignedPaperId) {
        this.assignedPaperId = assignedPaperId;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public AssessmentCandidateStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentCandidateStatus status) {
        this.status = status;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Instant blockedAt) {
        this.blockedAt = blockedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentCandidate target = new AssessmentCandidate();

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

        public Builder studentId(UUID studentId) {
            target.studentId = studentId;
            return this;
        }

        public Builder assignedPaperId(UUID assignedPaperId) {
            target.assignedPaperId = assignedPaperId;
            return this;
        }

        public Builder scheduleId(UUID scheduleId) {
            target.scheduleId = scheduleId;
            return this;
        }

        public Builder status(AssessmentCandidateStatus status) {
            target.status = status;
            return this;
        }

        public Builder assignedAt(Instant assignedAt) {
            target.assignedAt = assignedAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            target.updatedAt = updatedAt;
            return this;
        }

        public Builder blockedAt(Instant blockedAt) {
            target.blockedAt = blockedAt;
            return this;
        }

        public Builder assignedBy(UUID assignedBy) {
            target.assignedBy = assignedBy;
            return this;
        }

        public Builder updatedBy(UUID updatedBy) {
            target.updatedBy = updatedBy;
            return this;
        }

        public AssessmentCandidate build() {
            return target;
        }

    }
}
