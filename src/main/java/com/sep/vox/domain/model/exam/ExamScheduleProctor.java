package com.sep.vox.domain.model.exam;

import java.util.UUID;

public class ExamScheduleProctor {
    private UUID id;
    private UUID scheduleId;
    private UUID teacherId;

    public ExamScheduleProctor() {}

    public ExamScheduleProctor(UUID id, UUID scheduleId, UUID teacherId) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.teacherId = teacherId;
    }

    public ExamScheduleProctor(UUID scheduleId, UUID teacherId) {
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
