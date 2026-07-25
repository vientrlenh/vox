package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_schedules")
public class ExamScheduleJpaEntity {
    
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    // Nullable: CLASS_TEST không có phòng thi vật lý, dùng 1 ExamSchedule "ảo"
    // (schoolRoomId = null) để có mốc thời gian thống nhất với CENTRALIZED.
    @Column(name = "school_room_id", nullable = true)
    private UUID schoolRoomId;

    @Column(name = "start_date", nullable = false)
    private OffsetDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private OffsetDateTime endDate;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_schedules_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'COMPLETED', 'MOVED', 'CANCELLED', 'DELETED')"
        )
    })
    private String status;

    @Column(name = "moved_to_schedule_id")
    private UUID movedToScheduleId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected ExamScheduleJpaEntity() {}

    public ExamScheduleJpaEntity(UUID id, UUID examId, UUID schoolRoomId, OffsetDateTime startDate,
            OffsetDateTime endDate, String status, UUID movedToScheduleId, OffsetDateTime createdAt,
            OffsetDateTime updatedAt, UUID createdBy, UUID updatedBy) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
