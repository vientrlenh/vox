package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_schedule_proctors")
public class ExamScheduleProctorJpaEntity {
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

    @Column(name = "schedule_id", nullable = false, updatable = false)
    private UUID scheduleId;

    @Column(name = "teacher_id", nullable = false, updatable = false)
    private UUID teacherId;

    protected ExamScheduleProctorJpaEntity() {}

    public ExamScheduleProctorJpaEntity(UUID id, UUID scheduleId, UUID teacherId) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.teacherId = teacherId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(UUID scheduleId) {
        this.scheduleId = scheduleId;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    
}
