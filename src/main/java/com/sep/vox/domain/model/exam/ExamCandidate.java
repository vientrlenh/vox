package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamCandidate {
    private UUID id;
    private UUID examId;
    private UUID studentId;
    private UUID assignedPaperId;
    private UUID scheduleId;
    private ExamCandidateStatus status;
    private OffsetDateTime assignedAt;
    private OffsetDateTime updatedAt;
    private UUID assignedBy;
    private UUID updatedBy;

    public ExamCandidate() {}

    public ExamCandidate(UUID id, UUID examId, UUID studentId, UUID assignedPaperId, UUID scheduleId,
            ExamCandidateStatus status, OffsetDateTime assignedAt, OffsetDateTime updatedAt, UUID assignedBy,
            UUID updatedBy) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.assignedPaperId = assignedPaperId;
        this.scheduleId = scheduleId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.updatedAt = updatedAt;
        this.assignedBy = assignedBy;
        this.updatedBy = updatedBy;
    }

    public ExamCandidate(UUID examId, UUID studentId, UUID assignedPaperId, UUID scheduleId, ExamCandidateStatus status,
            OffsetDateTime assignedAt, OffsetDateTime updatedAt, UUID assignedBy, UUID updatedBy) {
        this.examId = examId;
        this.studentId = studentId;
        this.assignedPaperId = assignedPaperId;
        this.scheduleId = scheduleId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.updatedAt = updatedAt;
        this.assignedBy = assignedBy;
        this.updatedBy = updatedBy;
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

    public ExamCandidateStatus getStatus() {
        return status;
    }

    public void setStatus(ExamCandidateStatus status) {
        this.status = status;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
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

    
}
