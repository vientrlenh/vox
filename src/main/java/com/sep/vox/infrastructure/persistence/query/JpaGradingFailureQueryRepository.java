package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaGradingFailureQueryRepository implements GradingFailureQueryRepository {

    /**
     * The set of failed sessions in the window, with just enough columns for all three statements below to share ONE
     * definition.
     *
     * <p>Written once and prepended to each statement, instead of copying the WHERE clause three times: the summary
     * card, the group card and the session list must count exactly the same set, and with three copies of the same
     * condition a single drifting detail is enough for three numbers on the same screen to tell three different stories.
     */
    private static final String FAILED_SESSIONS_CTE = """
        WITH failed AS (
            SELECT
                s.id                                            AS session_id,
                s.submitted_at                                  AS failed_at,
                s.grading_error                                 AS error,
                s.grading_retry_count                           AS retry_count,
                vox_grading_error_signature(s.grading_error)    AS signature,
                s.assessment_id                                       AS assessment_id,
                e.school_id                                     AS school_id,
                e.status <> 'RESULTS_PUBLISHED'                 AS retryable
            FROM assessment_sessions s
            JOIN assessments e ON e.id = s.assessment_id
            WHERE s.status = 'GRADING_FAILED'
              AND s.submitted_at >= :fromInstant
              AND s.submitted_at < :toInstant
        )
        """;

    private static final String FAILED_SESSION_ROWS_SQL = """
        SELECT
            f.session_id,
            f.school_id,
            sc.name,
            sc.code,
            f.assessment_id,
            e.name,
            u.full_name,
            f.failed_at,
            f.retry_count,
            f.error,
            f.retryable,
            EXISTS (SELECT 1 FROM assessment_candidate_results r WHERE r.session_id = f.session_id)
        FROM failed f
        JOIN assessments e ON e.id = f.assessment_id
        JOIN assessment_sessions s ON s.id = f.session_id
        JOIN assessment_candidates c ON c.id = s.candidate_id
        LEFT JOIN schools sc ON sc.id = f.school_id
        LEFT JOIN users u ON u.id = c.student_id
        WHERE f.signature IS NOT DISTINCT FROM :signature
        ORDER BY f.failed_at DESC, f.session_id ASC
        LIMIT :pageSize OFFSET :offset
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code COUNT(DISTINCT signature)} IGNORES NULL, so the "unknown cause" group has to be added by hand --
     * without it a system where every session failed through the DLT path would report "0 causes" while the
     * screen below is showing a group.
     */
    @Override
    public GradingFailureTotalsDto countTotals(Instant from, Instant to) {
        Object[] row = (Object[]) em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT
                COUNT(*),
                COUNT(DISTINCT signature)
                    + CASE WHEN COUNT(*) FILTER (WHERE signature IS NULL) > 0 THEN 1 ELSE 0 END,
                COUNT(DISTINCT school_id),
                COUNT(*) FILTER (WHERE retryable)
            FROM failed
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getSingleResult();

        return new GradingFailureTotalsDto(toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
    }

    /**
     * {@code array_agg(...)[1]} takes the raw message of the MOST RECENT failed session as the representative sample.
     * The group is named by the normalized signature (uuids and numbers replaced by placeholders), which is not
     * readable -- the person on call needs to see a real error sentence.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GradingFailureGroupDto> findGroups(Instant from, Instant to, int limit) {
        List<Object[]> rows = em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT
                signature,
                (array_agg(error ORDER BY failed_at DESC))[1],
                COUNT(*),
                COUNT(DISTINCT school_id),
                COUNT(DISTINCT assessment_id),
                MIN(failed_at),
                MAX(failed_at),
                COUNT(*) FILTER (WHERE retryable)
            FROM failed
            GROUP BY signature
            ORDER BY COUNT(*) DESC, MAX(failed_at) DESC
            LIMIT :maxGroups
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("maxGroups", limit)
            .getResultList();

        return rows.stream().map(JpaGradingFailureQueryRepository::toFailureGroup).toList();
    }

    private static GradingFailureGroupDto toFailureGroup(Object[] row) {
        return new GradingFailureGroupDto(
            (String) row[0],
            (String) row[1],
            toLong(row[2]),
            toLong(row[3]),
            toLong(row[4]),
            toInstant(row[5]),
            toInstant(row[6]),
            toLong(row[7]));
    }

    /**
     * {@code IS NOT DISTINCT FROM} and not {@code =}: the "unknown cause" group has a null signature,
     * and {@code null = null} in SQL gives null, so clicking that group would return an empty list
     * while the group card still shows the session count.
     *
     * <p>Sorting also uses {@code session_id} as a secondary key: several sessions of the same failed assessment shift fall in
     * the same second, and sorting by time alone leaves their order undefined — pagination would
     * repeat a session on one page and drop one on another.
     */
    @Override
    public PageResult<GradingFailureSessionDto> findSessions(
            Instant from, Instant to, String signature, int page, int size) {
        long total = countFailedSessions(from, to, signature);

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<GradingFailureSessionDto> content = findFailedSessionRows(from, to, signature, page, size).stream()
            .map(JpaGradingFailureQueryRepository::toFailureSession)
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    private long countFailedSessions(Instant from, Instant to, String signature) {
        return toLong(em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT COUNT(*) FROM failed WHERE signature IS NOT DISTINCT FROM :signature
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("signature", signature)
            .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findFailedSessionRows(Instant from, Instant to, String signature, int page, int size) {
        return em.createNativeQuery(FAILED_SESSIONS_CTE + FAILED_SESSION_ROWS_SQL)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("signature", signature)
            .setParameter("pageSize", size)
            .setParameter("offset", OneBasedPaging.toOffset(page, size))
            .getResultList();
    }

    private static GradingFailureSessionDto toFailureSession(Object[] row) {
        return new GradingFailureSessionDto(
            toUuid(row[0]),
            toUuid(row[1]),
            (String) row[2],
            (String) row[3],
            toUuid(row[4]),
            (String) row[5],
            (String) row[6],
            toInstant(row[7]),
            row[8] == null ? null : ((Number) row[8]).intValue(),
            (String) row[9],
            Boolean.TRUE.equals(row[10]),
            Boolean.TRUE.equals(row[11]));
    }

}
