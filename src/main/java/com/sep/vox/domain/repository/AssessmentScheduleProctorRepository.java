package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentScheduleProctor;

public interface AssessmentScheduleProctorRepository {
    AssessmentScheduleProctor save(AssessmentScheduleProctor proctor);
    Optional<AssessmentScheduleProctor> findById(UUID id);
    List<AssessmentScheduleProctor> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);
    void deleteById(UUID id);

    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);
    List<UUID> findScheduleIdsByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);

    /**
     * Whether this teacher already proctors another schedule (DRAFT/PUBLISHED) that overlaps in time with [start, end).
     * Scans EVERY assessment, not only the open one: a person cannot be present at two schedules at the same time.
     * {@code excludeScheduleId} (if not null) is excluded from the check.
     */
    boolean existsOverlappingAssignment(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId);

    /**
     * For each teacher in {@code teacherIds}, the first schedule (DRAFT/PUBLISHED) they proctor that
     * overlaps in time with [start, end) — used by the proctor selection screen to warn in advance who is busy.
     * A teacher who is free is not present in the result.
     */
    List<ProctorScheduleConflict> findConflictsForTeachers(
            Collection<UUID> teacherIds, Instant start, Instant end, UUID excludeScheduleId);

    /** One conflict: teacher {@code teacherId} already proctors schedule {@code scheduleId} running [start, end). */
    record ProctorScheduleConflict(UUID teacherId, UUID scheduleId, Instant startDate, Instant endDate) {
    }
}
