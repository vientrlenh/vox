package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamSchedule {
    private UUID id;
    private UUID examId;
    private UUID schoolRoomId;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private ExamScheduleStatus status;
    private UUID movedToScheduleId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamSchedule() {}

    public ExamSchedule(UUID id, UUID examId, UUID schoolRoomId, OffsetDateTime startDate, OffsetDateTime endDate,
            ExamScheduleStatus status, UUID movedToScheduleId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.examId = examId;
        this.schoolRoomId = schoolRoomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.movedToScheduleId = movedToScheduleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public ExamSchedule(UUID examId, UUID schoolRoomId, OffsetDateTime startDate, OffsetDateTime endDate,
            ExamScheduleStatus status, UUID movedToScheduleId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
            UUID createdBy, UUID updatedBy) {
        this.examId = examId;
        this.schoolRoomId = schoolRoomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.movedToScheduleId = movedToScheduleId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    /**
     * Tạo mới một ca thi ở trạng thái DRAFT (id do DB sinh).
     */
    public static ExamSchedule createFresh(UUID examId, UUID schoolRoomId, OffsetDateTime startDate,
            OffsetDateTime endDate, UUID createdBy, OffsetDateTime now) {
        return new ExamSchedule(examId, schoolRoomId, startDate, endDate,
                ExamScheduleStatus.DRAFT, null, now, now, createdBy, createdBy);
    }

    /**
     * Ca thi chỉ được phép sửa (đổi phòng/giờ) khi còn ở trạng thái DRAFT.
     */
    public boolean isModifiable() {
        return this.status == ExamScheduleStatus.DRAFT;
    }

    public UUID getId() {
        return this.id;
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

    public UUID getSchoolRoomId() {
        return schoolRoomId;
    }

    public void setSchoolRoomId(UUID schoolRoomId) {
        this.schoolRoomId = schoolRoomId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public ExamScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ExamScheduleStatus status) {
        this.status = status;
    }

    public UUID getMovedToScheduleId() {
        return movedToScheduleId;
    }

    public void setMovedToScheduleId(UUID movedToScheduleId) {
        this.movedToScheduleId = movedToScheduleId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    
}
