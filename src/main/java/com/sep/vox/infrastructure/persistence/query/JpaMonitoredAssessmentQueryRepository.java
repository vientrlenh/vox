package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.query.repository.MonitoredAssessmentQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaMonitoredAssessmentQueryRepository implements MonitoredAssessmentQueryRepository {

    /**
     * Sessions that may have something to monitor.
     *
     * <p>Excludes DRAFT (nobody can sit an unpublished one), CANCELLED, and also DELETED/MOVED. COMPLETED is kept
     * because the time window below already filters for it: a session just marked completed while students are
     * still talking must still be openable, and on the monitoring screen missing a room that is running badly is
     * far worse than showing one extra row.
     */
    private static final Set<String> MONITORABLE_STATUSES = Set.of(
        AssessmentScheduleStatus.PUBLISHED.name(),
        AssessmentScheduleStatus.COMPLETED.name());

    private static final String MONITORED_SELECT = """
        SELECT NEW com.sep.vox.application.query.dto.MonitoredAssessmentSummary(
            assessment.id,
            assessment.code,
            assessment.name,
            assessment.kind,
            assessment.status,
            MIN(sch.startDate),
            MAX(sch.endDate),
            SUM(CASE WHEN sch.startDate <= :now AND sch.endDate > :now THEN 1L ELSE 0L END)
        )
        FROM AssessmentScheduleJpaEntity sch
        JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
        """;

    private static final String MONITORED_FILTER = """
        WHERE sch.status IN :statuses
          AND assessment.status <> 'DRAFT'
          AND (:assessmentId IS NULL OR assessment.id = :assessmentId)
        """;

    private static final String MONITORED_GROUPING = """
        GROUP BY assessment.id, assessment.code, assessment.name, assessment.kind, assessment.status
        ORDER BY MIN(sch.startDate)
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<MonitoredAssessmentSummary> findMonitorableByTeacher(
            UUID teacherId, UUID assessmentId, Instant now, Instant leadUntil) {
        return query("""
                JOIN AssessmentScheduleProctorJpaEntity p
                    ON p.scheduleId = sch.id AND p.teacherId = :teacherId
            """, "", assessmentId, now, leadUntil)
            .setParameter("teacherId", teacherId)
            .getResultList();
    }

    @Override
    public List<MonitoredAssessmentSummary> findMonitorableBySchool(
            UUID schoolId, UUID assessmentId, Instant now, Instant leadUntil) {
        return query("", " AND assessment.schoolId = :schoolId\n", assessmentId, now, leadUntil)
            .setParameter("schoolId", schoolId)
            .getResultList();
    }

    /** Build the statement and bind the shared parameters; the two read paths only have to bind the authorization evidence. */
    private TypedQuery<MonitoredAssessmentSummary> query(
            String accessJoin, String accessWhere, UUID assessmentId, Instant now, Instant leadUntil) {
        TypedQuery<MonitoredAssessmentSummary> query = em
            .createQuery(select(accessJoin, accessWhere, leadUntil != null), MonitoredAssessmentSummary.class)
            .setParameter("assessmentId", assessmentId)
            .setParameter("now", now)
            .setParameter("statuses", MONITORABLE_STATUSES);
        if (leadUntil != null) {
            query.setParameter("leadUntil", leadUntil);
        }
        return query;
    }

    /**
     * The shared body of the two read paths; they differ only in how permission is proven.
     *
     * <p>Merged because the rest -- the time window, the grouping by assessment, the way running sessions are counted --
     * must be exactly identical: a proctor and a school admin looking at the same assessment room must not see two
     * different numbers.
     *
     * <p>Filtering on the SESSION status alone is not enough, hence the extra {@code assessment.status <> 'DRAFT'}: the two status
     * columns are two independent machines, and the standard flow requires publishing each session FIRST and only then pushing the assessment
     * {@code DRAFT -> SCHEDULED}, so a PUBLISHED session under an assessment that is still DRAFT is valid data --
     * without it both roles would see an assessment the school has not published. Only DRAFT is hidden: a CANCELLED assessment
     * is still shown with its status, the same rule as {@code JpaProctorScheduleQueryRepository}.
     *
     * @param windowed whether the time window is SPLICED INTO or dropped entirely from the statement -- deliberately not written as
     *                 {@code :leadUntil IS NULL OR ...} like the filter by assessmentId right below.
     *                 The first version was written that way and blew up on Postgres: {@code could not determine data type of
     *                 parameter $8}. The pgjdbc driver sends a time parameter WITHOUT a type OID so the server
     *                 chooses between timestamp and timestamptz itself, so a bare {@code $8 IS NULL} leaves
     *                 nothing from which to infer the type. {@code :assessmentId IS NULL} survives because UUID has its own
     *                 OID -- the same way of writing it, only a different Java type, so this is a silent trap:
     *                 it passes both compilation and Hibernate's translation of JPQL to SQL.
     */
    private String select(String accessJoin, String accessWhere, boolean windowed) {
        return MONITORED_SELECT
            + accessJoin
            + MONITORED_FILTER
            + (windowed ? "  AND sch.startDate <= :leadUntil AND sch.endDate > :now\n" : "")
            + accessWhere
            + MONITORED_GROUPING;
    }
}
