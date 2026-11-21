package com.sep.vox.domain.model.assessment;

import java.util.UUID;

public class AssessmentScheduleProctor {
    private UUID id;
    private UUID scheduleId;
    private UUID teacherId;

    public AssessmentScheduleProctor() {}

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentScheduleProctor target = new AssessmentScheduleProctor();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder scheduleId(UUID scheduleId) {
            target.scheduleId = scheduleId;
            return this;
        }

        public Builder teacherId(UUID teacherId) {
            target.teacherId = teacherId;
            return this;
        }

        public AssessmentScheduleProctor build() {
            return target;
        }

    }
}
