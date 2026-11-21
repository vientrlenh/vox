package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_schedules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentScheduleJpaEntity extends BaseEntity {

    @Column(name = "assessment_id", nullable = false, updatable = false)
    private UUID assessmentId;

    // Nullable: CLASS_TEST has no physical assessment room and uses 1 "virtual" AssessmentSchedule
    // (schoolRoomId = null) to have a time anchor consistent with CENTRALIZED.
    @Column(name = "school_room_id", nullable = true)
    private UUID schoolRoomId;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_schedules_status_valid", 
            constraint = "status IN ('DRAFT', 'PUBLISHED', 'COMPLETED', 'MOVED', 'CANCELLED', 'DELETED')"
        )
    })
    private String status;

    @Column(name = "moved_to_schedule_id")
    private UUID movedToScheduleId;
}
