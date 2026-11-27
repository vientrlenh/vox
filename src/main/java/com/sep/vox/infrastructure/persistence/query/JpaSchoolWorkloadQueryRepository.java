package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toInt;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AssessmentAwaitingPublishDto;
import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolWorkloadQueryRepository implements SchoolWorkloadQueryRepository {

    /**
     * ONE definition of "an attempt without a score", shared by both statements below.
     *
     * <p>Two ways into this set, and they are mutually exclusive by construction:
     *
     * <ul>
     *   <li>A {@code GRADING_FAILED} session with NO result row yet -- the AI failed and nobody has touched it.
     *   <li>A {@code PENDING_REVIEW} result row -- the attempt is in the grader queue, whether the session is
     *       {@code GRADED} or still carries {@code GRADING_FAILED}. Handing off to manual grading DELIBERATELY does
     *       not change the session status (see {@code HandOffGradingToHumanUseCase}), so looking at the session
     *       status alone would count an attempt someone has already taken as one nobody is handling.
     * </ul>
     *
     * <p>Periods that are already {@code RESULTS_PUBLISHED} are EXCLUDED: both AI re-grading and changing the grader
     * are rejected in that state, so those attempts are no longer work to do but damage that has already happened --
     * leaving them in the queue would give the number a floor that can never return to 0. They still show on the
     * session detail screen, with the reason why no button is left.
     *
     * <p>The {@code LEFT JOIN} to the assignment can produce at most one row: {@code active_result_id} is the
     * "open assignment" flag, and each attempt has at most one open round.
     */
    private static final String UNSCORED_CTE = """
        WITH unscored AS (
            SELECT
                s.id           AS session_id,
                s.assessment_id      AS assessment_id,
                s.submitted_at AS submitted_at,
                CASE
                    WHEN r.id IS NULL AND s.school_regrade_count < :maxSchoolRegrades
                        THEN 'AI_RETRY_LEFT'
                    WHEN r.id IS NULL
                        THEN 'AI_NO_RETRY'
                    WHEN ga.id IS NULL
                        THEN 'AWAITING_ASSIGNMENT'
                    WHEN ga.deadline_at IS NOT NULL AND ga.deadline_at < :now
                        THEN 'ASSIGNED_OVERDUE'
                    ELSE 'ASSIGNED_IN_PROGRESS'
                END AS bucket
            FROM assessment_sessions s
            JOIN assessments e ON e.id = s.assessment_id
            LEFT JOIN assessment_candidate_results r
                   ON r.session_id = s.id
                  AND r.status <> 'DELETED'
            LEFT JOIN assessment_grading_assignments ga
                   ON ga.active_result_id = r.id
            WHERE e.school_id = :schoolId
              AND e.kind = 'CENTRALIZED'
              AND e.status NOT IN ('CANCELLED', 'RESULTS_PUBLISHED')
              AND s.status <> 'DELETED'
              AND (
                    (s.status = 'GRADING_FAILED' AND r.id IS NULL)
                 OR r.status = 'PENDING_REVIEW'
              )
        )
        """;

    private static final String UNHANDLED_AI_FAILURES_SELECT = """
        SELECT
            u.session_id,
            u.assessment_id,
            e.code,
            e.name,
            usr.full_name,
            (SELECT sc.name
               FROM school_class_users scu
               JOIN school_classes sc ON sc.id = scu.school_class_id
              WHERE scu.user_id = c.student_id AND scu.is_active = TRUE
              ORDER BY sc.name ASC
              LIMIT 1),
            u.submitted_at,
            s.grading_error,
            s.grading_retry_count,
            u.bucket = 'AI_RETRY_LEFT'
        FROM unscored u
        JOIN assessment_sessions s ON s.id = u.session_id
        JOIN assessments e ON e.id = u.assessment_id
        JOIN assessment_candidates c ON c.id = s.candidate_id
        LEFT JOIN users usr ON usr.id = c.student_id
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code MIN(submitted_at)} returns null when no attempt is left -- and that null must travel all the way to
     * the UI: "empty queue" is very different from "there is an attempt, just submitted today", which also gives
     * 0 days of waiting.
     */
    @Override
    public SchoolUnscoredWorkloadDto countUnscored(UUID schoolId, Instant now) {
        Object[] row = (Object[]) em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                COUNT(*) FILTER (WHERE bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE bucket = 'AI_NO_RETRY'),
                COUNT(*) FILTER (WHERE bucket = 'AWAITING_ASSIGNMENT'),
                COUNT(*) FILTER (WHERE bucket = 'ASSIGNED_OVERDUE'),
                COUNT(*) FILTER (WHERE bucket = 'ASSIGNED_IN_PROGRESS'),
                MIN(submitted_at),
                COUNT(DISTINCT assessment_id)
            FROM unscored
            """)
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .setParameter("maxSchoolRegrades", AssessmentSession.MAX_SCHOOL_REGRADES)
            .getSingleResult();

        return new SchoolUnscoredWorkloadDto(
            toInt(row[0]), toInt(row[1]), toInt(row[2]), toInt(row[3]), toInt(row[4]),
            toInstant(row[5]), toInt(row[6]));
    }

    /**
     * Only {@code CLOSED} periods: exactly the set of periods for which the "Publish scores" button is clickable.
     * A period that is still {@code IN_PROGRESS} still has people taking the test, so empty attempts are normal
     * and not yet a warning.
     */
    @Override
    public List<AssessmentAwaitingPublishDto> findAssessmentsAwaitingPublish(UUID schoolId, Instant now, int limit) {
        return findAwaitingPublishRows(schoolId, now, limit).stream()
            .map(JpaSchoolWorkloadQueryRepository::toAwaitingPublish)
            .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findAwaitingPublishRows(UUID schoolId, Instant now, int limit) {
        return em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                e.id,
                e.code,
                e.name,
                e.close_at,
                COUNT(*),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_NO_RETRY'),
                COUNT(*) FILTER (WHERE u.bucket IN
                    ('AWAITING_ASSIGNMENT', 'ASSIGNED_OVERDUE', 'ASSIGNED_IN_PROGRESS'))
            FROM unscored u
            JOIN assessments e ON e.id = u.assessment_id
            WHERE e.status = 'CLOSED'
            GROUP BY e.id, e.code, e.name, e.close_at
            ORDER BY COUNT(*) DESC, e.close_at ASC NULLS LAST, e.id ASC
            LIMIT :maxAssessments
            """)
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .setParameter("maxSchoolRegrades", AssessmentSession.MAX_SCHOOL_REGRADES)
            .setParameter("maxAssessments", limit)
            .getResultList();
    }

    private static AssessmentAwaitingPublishDto toAwaitingPublish(Object[] row) {
        return new AssessmentAwaitingPublishDto(
            toUuid(row[0]),
            (String) row[1],
            (String) row[2],
            toInstant(row[3]),
            toInt(row[4]),
            toInt(row[5]),
            toInt(row[6]),
            toInt(row[7]));
    }

    /**
     * Runs on {@code unscored} ITSELF and then filters for the two AI groups, instead of rewriting the "failed session
     * with no result" clause: the card on the overview page and this list must count exactly one set, and a
     * copy of the same condition only has to differ in one detail for someone to click the number 14 and see 12 rows.
     *
     * <p>Sorted OLDEST FIRST — the opposite of the system admin's screen. There the question is "what just
     * broke", here it is "which student has waited longest and still has no score".
     *
     * <p>{@code now} must still be passed into the CTE even though the two AI groups do not use it: it is in the overdue
     * branch of the other groups, and Hibernate requires every parameter in the statement to be bound.
     */
    @Override
    public PageResult<SchoolGradingFailureDto> findUnhandledAiFailures(
            UUID schoolId, UUID assessmentId, Boolean retryLeft, int page, int size) {
        String where = unhandledAiFailureFilter(assessmentId, retryLeft);

        long total = toLong(bind(em.createNativeQuery(
            UNSCORED_CTE + "SELECT COUNT(*) FROM unscored u" + where), schoolId, assessmentId)
            .getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<SchoolGradingFailureDto> content =
            findUnhandledAiFailureRows(schoolId, assessmentId, where, page, size).stream()
                .map(JpaSchoolWorkloadQueryRepository::toGradingFailure)
                .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    /**
     * The trailing line break is REQUIRED: the clause may end with a named parameter (:assessmentId), and
     * concatenating it directly with the following "ORDER BY" would produce the parameter name ":assessmentIdORDER"
     * -- Hibernate reports "no parameter named" instead of an SQL syntax error.
     */
    private static String unhandledAiFailureFilter(UUID assessmentId, Boolean retryLeft) {
        return " WHERE u.bucket IN ('AI_RETRY_LEFT', 'AI_NO_RETRY')"
            + (assessmentId == null ? "" : " AND u.assessment_id = :assessmentId")
            + allowancePredicate(retryLeft)
            + "\n";
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findUnhandledAiFailureRows(
            UUID schoolId, UUID assessmentId, String where, int page, int size) {
        return bind(em.createNativeQuery(UNSCORED_CTE + UNHANDLED_AI_FAILURES_SELECT + where + """
            ORDER BY u.submitted_at ASC, u.session_id ASC
            LIMIT :pageSize OFFSET :offset
            """), schoolId, assessmentId)
            .setParameter("pageSize", size)
            .setParameter("offset", OneBasedPaging.toOffset(page, size))
            .getResultList();
    }

    private static SchoolGradingFailureDto toGradingFailure(Object[] row) {
        return new SchoolGradingFailureDto(
            toUuid(row[0]),
            toUuid(row[1]),
            (String) row[2],
            (String) row[3],
            (String) row[4],
            (String) row[5],
            toInstant(row[6]),
            (String) row[7],
            row[8] == null ? null : ((Number) row[8]).intValue(),
            Boolean.TRUE.equals(row[9]));
    }

    @Override
    public int[] countAiFailuresByAllowance(UUID schoolId, UUID assessmentId) {
        Object[] row = (Object[]) bind(em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                COUNT(*) FILTER (WHERE u.bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_NO_RETRY')
            FROM unscored u
            """ + (assessmentId == null ? "" : " WHERE u.assessment_id = :assessmentId")), schoolId, assessmentId)
            .getSingleResult();

        return new int[] { toInt(row[0]), toInt(row[1]) };
    }

    /**
     * Spliced straight into the SQL string rather than passed as a nullable Boolean parameter: Postgres cannot
     * infer the type of a lone null parameter and throws "could not determine data type of parameter". The spliced
     * value is a constant written by this class, not user data.
     */
    private static String allowancePredicate(Boolean retryLeft) {
        if (retryLeft == null) {
            return "";
        }
        return retryLeft ? " AND u.bucket = 'AI_RETRY_LEFT'" : " AND u.bucket = 'AI_NO_RETRY'";
    }

    private Query bind(Query query, UUID schoolId, UUID assessmentId) {
        query.setParameter("schoolId", schoolId)
            .setParameter("now", Instant.now())
            .setParameter("maxSchoolRegrades", AssessmentSession.MAX_SCHOOL_REGRADES);
        if (assessmentId != null) {
            query.setParameter("assessmentId", assessmentId);
        }
        return query;
    }
}
