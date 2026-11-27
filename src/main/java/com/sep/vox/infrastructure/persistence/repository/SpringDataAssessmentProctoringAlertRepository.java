package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentProctoringAlertJpaEntity;

public interface SpringDataAssessmentProctoringAlertRepository extends JpaRepository<AssessmentProctoringAlertJpaEntity, UUID> {

    boolean existsByEventId(String eventId);

    List<AssessmentProctoringAlertJpaEntity> findByAssessmentSessionIdOrderByCapturedAtAsc(UUID assessmentSessionId);

    /**
     * Whether this session has any alert in the group that raises a doubt about the INTEGRITY of the attempt.
     *
     * <p>{@code exists} instead of loading the list and then filtering: the question here is yes/no, and the function is called
     * once per answer when the grader writes its result -- an attempt of 4 questions is 4 calls. Loading
     * the whole list only to throw it away pays for something that is not used.
     *
     * <p>{@code UPPER(...)} because the source that writes the alerts is an external event, and nothing guarantees it is always
     * properly upper-cased -- the same reason the previous version compared {@code level} in an IgnoreCase way. The list
     * passed in is already upper-case (see {@code AssessmentProctoringAlert.INTEGRITY_ALERT_TYPES}).
     *
     * <p>The previous query asked by {@code level = 'CRITICAL'}; for why it changed to asking by TYPE, see the javadoc
     * of {@code INTEGRITY_ALERT_TYPES}.
     */
    @Query("""
        SELECT COUNT(alert) > 0 FROM AssessmentProctoringAlertJpaEntity alert
        WHERE alert.assessmentSessionId = :assessmentSessionId
          AND UPPER(TRIM(alert.alertType)) IN :alertTypes
    """)
    boolean existsIntegrityAlert(
        @Param("assessmentSessionId") UUID assessmentSessionId,
        @Param("alertTypes") Collection<String> alertTypes);

    /**
     * All alerts of an assessment schedule, merged across all sessions in the schedule.
     *
     * <p>The alert table only knows the session, while the schedule is two hops away
     * (session -> candidate -> schedule), so JPQL must be written instead of relying on the method name. A subquery is used
     * rather than a direct join so that the result is not duplicated if the middle hop returns several rows.
     */
    @Query("""
        SELECT alert FROM AssessmentProctoringAlertJpaEntity alert
        WHERE alert.assessmentSessionId IN (
            SELECT session.id FROM AssessmentSessionJpaEntity session
            JOIN AssessmentCandidateJpaEntity candidate ON candidate.id = session.candidateId
            WHERE candidate.scheduleId = :scheduleId
        )
        ORDER BY alert.capturedAt ASC
    """)
    List<AssessmentProctoringAlertJpaEntity> findByScheduleIdOrderByCapturedAtAsc(@Param("scheduleId") UUID scheduleId);
}
