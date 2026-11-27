package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentScheduleProctorJpaEntity;

public interface SpringDataAssessmentScheduleProctorRepository extends JpaRepository<AssessmentScheduleProctorJpaEntity, UUID> {
    List<AssessmentScheduleProctorJpaEntity> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);

    List<AssessmentScheduleProctorJpaEntity> findByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);
    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);

    /**
     * Counts the sessions still in effect that the teacher has proctored and whose time intersects [start, end).
     *
     * <p>It now lives on the parent table so it has to join to {@code assessment_schedules}. The interval intersection condition uses
     * exactly the half-open form of {@code SpringDataAssessmentScheduleRepository.countOverlapping}: two adjacent sessions
     * (the earlier one ending exactly when the later one starts) do NOT count as overlapping.
     */
    @Query("""
        SELECT COUNT(p) FROM AssessmentScheduleProctorJpaEntity p
        JOIN AssessmentScheduleJpaEntity s ON s.id = p.scheduleId
        WHERE p.teacherId = :teacherId
          AND s.status IN ('DRAFT', 'PUBLISHED')
          AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
          AND s.startDate < :end AND s.endDate > :start
        """)
    long countOverlappingAssignments(
        @Param("teacherId") UUID teacherId,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("excludeScheduleId") UUID excludeScheduleId);

    /** Same filter as above but for a whole group of teachers, and returns the conflicting schedule. */
    @Query("""
        SELECT p.teacherId, s.id, s.startDate, s.endDate
        FROM AssessmentScheduleProctorJpaEntity p
        JOIN AssessmentScheduleJpaEntity s ON s.id = p.scheduleId
        WHERE p.teacherId IN :teacherIds
          AND s.status IN ('DRAFT', 'PUBLISHED')
          AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
          AND s.startDate < :end AND s.endDate > :start
        ORDER BY s.startDate
        """)
    List<Object[]> findOverlappingAssignments(
        @Param("teacherIds") Collection<UUID> teacherIds,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("excludeScheduleId") UUID excludeScheduleId);
}
