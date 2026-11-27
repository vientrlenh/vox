package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;

public interface AssessmentScheduleRepository {
    Optional<AssessmentSchedule> findById(UUID id);

    /**
     * (manual candidate assignment / auto-fill). Used within the same transaction.
     */
    Optional<AssessmentSchedule> findByIdForUpdate(UUID id);

    /** Schedules of a class test (excluding DELETED ones). */
    List<AssessmentSchedule> findByAssessmentId(UUID assessmentId);

    /**
     * The sessions of many assessments at once (DELETED sessions excluded) -- used by the DataLoader
     * {@code assessmentSchedulesByAssessmentId} so the assessment list page can build its progress bar without N+1.
     */
    List<AssessmentSchedule> findByAssessmentIdIn(Collection<UUID> assessmentIds);

    /** Schedules of the whole school (excluding DELETED ones). */
    List<AssessmentSchedule> findBySchoolId(UUID schoolId);

    /** Finds several schedules by id (used for the DataLoader batch). */
    List<AssessmentSchedule> findByIdIn(java.util.Collection<UUID> ids);

    /**
     * The ids of EVERY session of the assessment, including soft-deleted ones -- used when cleaning up child data when hard-deleting
     * an assessment. {@link #findByAssessmentId(UUID)} filters out DELETED so using it there would miss the proctors of
     * the soft-deleted sessions.
     */
    List<UUID> findAllIdsByAssessmentId(UUID assessmentId);

    /** Hard-deletes every schedule of the assessment (including soft-deleted ones). */
    void deleteByAssessmentId(UUID assessmentId);

    AssessmentSchedule save(AssessmentSchedule schedule);

    /**
     * Whether any schedule (DRAFT/PUBLISHED) uses the same room and overlaps in time with the range [start, end).
     * {@code excludeScheduleId} (if not null) is excluded from the check (used when editing that schedule itself).
     */
    boolean existsOverlapping(UUID schoolRoomId, Instant start, Instant end, UUID excludeScheduleId);

    /**
     * Atomically updates the room/time of a schedule. Applied only while the schedule is DRAFT (WHERE ... status='DRAFT').
     * Returns the number of affected rows. The adapter writes the audit values itself, because a bulk update bypasses
     * the auditing listener.
     */
    int updateAtomic(UUID id, UUID schoolRoomId, boolean schoolRoomIdProvided, Instant start, Instant end);
    /**
     * Schedules that are published but already past their end time, oldest first, at most {@code limit} rows — used by
     * the job that moves a schedule to COMPLETED automatically. Capped because the first run after deployment has to sweep the whole
     * historical backlog.
     */
    List<AssessmentSchedule> findPublishedEndedBefore(Instant now, int limit);

    List<AssessmentSchedule> findByAssessmentIdAndInSchedule(UUID assessmentId, Instant now);
    List<AssessmentSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now);
    Optional<AssessmentSchedule> findByIdAndInSchedule(UUID id, Instant now);
    List<AssessmentSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);
}
