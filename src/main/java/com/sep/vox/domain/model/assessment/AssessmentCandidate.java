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

    protected AssessmentCandidate() {}

    public AssessmentCandidate(UUID id, UUID assessmentId, UUID studentId, UUID assignedPaperId, UUID scheduleId,
            AssessmentCandidateStatus status, Instant assignedAt, Instant updatedAt, Instant blockedAt, UUID assignedBy,
            UUID updatedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.assignedPaperId = assignedPaperId;
        this.scheduleId = scheduleId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.updatedAt = updatedAt;
        this.blockedAt = blockedAt;
        this.assignedBy = assignedBy;
        this.updatedBy = updatedBy;
    }

    /**
     * Tạo mới một thí sinh cho kỳ thi ở trạng thái ASSIGNED (id do DB sinh), chưa gán ca và chưa gán đề.
     */
    public static AssessmentCandidate createFresh(UUID assessmentId, UUID studentId, UUID createdBy, Instant now) {
        return new AssessmentCandidate(assessmentId, studentId, null, null,
                AssessmentCandidateStatus.ASSIGNED, now, now, null, createdBy, createdBy);
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
}
