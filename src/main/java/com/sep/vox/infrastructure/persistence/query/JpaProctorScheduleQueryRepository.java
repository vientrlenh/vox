package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * The list of sessions for the attendance screen, for proctors and for the school administration.
 *
 * <p>The two statements below filter by TWO status columns, not one. {@code assessments.status} and
 * {@code assessment_schedules.status} are two independent state machines, and the standard flow requires the scheduler to
 * publish each session FIRST and only then push the assessment {@code DRAFT -> SCHEDULED} -- {@code UpdateAssessmentStatusUseCase}
 * refuses to schedule while a DRAFT session remains. So "a PUBLISHED session under a DRAFT assessment" is a state every
 * assessment passes through, not corrupt data: dropping {@code assessment.status <> 'DRAFT'} makes the attendance screen show
 * an assessment the school has not published, exactly the bug that happened once.
 *
 * <p>Only DRAFT is hidden, the same rule as {@code JpaStudentAssessmentQueryRepository} and
 * {@code ViewMyAssessmentSchedulesUseCase}: a CANCELLED assessment is still shown so users know it was cancelled.
 */
@Repository
public class JpaProctorScheduleQueryRepository implements ProctorScheduleQueryRepository {

    /**
     * Sessions that must not show on the attendance screen.
     *
     * <p>DELETED/MOVED are clear: soft-deleted or moved to another session so they are no longer real sessions. DRAFT is a
     * session that is NOT published yet -- publishing is the moment the system requires enough proctors and every candidate to have a paper
     * (see {@code UpdateAssessmentScheduleStatusUseCase#publish}), so before that the session is still being arranged and there is
     * nothing to take attendance of. The same rule as {@code JpaMonitoredAssessmentQueryRepository}; two reads of the same
     * screen must not count differently.
     *
     * <p>CANCELLED is deliberately still shown, with its status, so a proctor knows the session was cancelled and does not go to the room.
     */
    private static final Set<String> HIDDEN_SCHEDULE_STATUSES = Set.of(
        AssessmentScheduleStatus.DRAFT.name(),
        AssessmentScheduleStatus.DELETED.name(),
        AssessmentScheduleStatus.MOVED.name());

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProctorScheduleSummary> findByTeacherId(UUID teacherId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                assessment.id,
                assessment.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM AssessmentScheduleJpaEntity sch
            JOIN AssessmentScheduleProctorJpaEntity proctor
                ON proctor.scheduleId = sch.id AND proctor.teacherId = :teacherId
            JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE sch.status NOT IN :hiddenStatuses
                AND assessment.status <> 'DRAFT'
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("teacherId", teacherId)
            .setParameter("hiddenStatuses", HIDDEN_SCHEDULE_STATUSES)
            .getResultList();
    }

    @Override
    public List<ProctorScheduleSummary> findBySchoolId(UUID schoolId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                assessment.id,
                assessment.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM AssessmentScheduleJpaEntity sch
            JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE assessment.schoolId = :schoolId
                AND sch.status NOT IN :hiddenStatuses
                AND assessment.status <> 'DRAFT'
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("schoolId", schoolId)
            .setParameter("hiddenStatuses", HIDDEN_SCHEDULE_STATUSES)
            .getResultList();
    }
}
