package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.StudentAssessmentRowInfo;
import com.sep.vox.application.query.repository.StudentAssessmentQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaStudentAssessmentQueryRepository implements StudentAssessmentQueryRepository {

    /**
     * Higher than the 100 cap of the other query repositories because the desktop client deliberately asks for ONE page big enough
     * ({@code /api/v1/assessments?page=1&size=200}) and then lists in place instead of paging inside the app.
     * Cutting it down to 100 here would silently hide the assessments of the 101st student.
     */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * The sessions a student is allowed to see -- kept in line with
     * {@code AssessmentScheduleStatus#isVisibleToStudent()}. DRAFT is a session not yet published, MOVED/DELETED have been
     * replaced; CANCELLED is still shown so the student knows the session was cancelled.
     */
    private static final String VISIBLE_SCHEDULE_STATUSES = "'PUBLISHED', 'COMPLETED', 'CANCELLED'";

    /**
     * The SQL version of {@code StudentAssessmentViewSupport#statusOf}. It must match the Java version branch by branch:
     * it is both a returned column and a filter predicate, so one branch off and the list and the counter disagree.
     * A class test derives from the assessment status, a centralized assessment derives from the session time.
     */
    private static final String DERIVED_STATUS = """
        CASE
            WHEN e.kind = 'CLASS_TEST' AND e.status = 'IN_PROGRESS' THEN 'in_progress'
            WHEN e.kind = 'CLASS_TEST' AND (e.status = 'DRAFT' OR e.status = 'SCHEDULED') THEN 'upcoming'
            WHEN e.kind = 'CLASS_TEST' THEN 'completed'
            WHEN s.startDate IS NULL OR s.endDate IS NULL THEN 'upcoming'
            WHEN s.startDate <= :now AND s.endDate > :now THEN 'in_progress'
            WHEN s.startDate > :now THEN 'upcoming'
            ELSE 'completed'
        END
        """;

    /**
     * INNER JOIN sang schedule là cố ý: bản Java loại thẳng thí sinh chưa xếp ca
     * ({@code scheduleId == null}) hoặc ca đã biến mất, nên join trong đã đúng ngữ nghĩa đó.
     */
    private static final String FROM_WHERE = """
        FROM AssessmentCandidateJpaEntity c
        JOIN AssessmentJpaEntity e ON e.id = c.assessmentId
        JOIN AssessmentScheduleJpaEntity s ON s.id = c.scheduleId
        WHERE c.studentId = :studentId
          AND e.status <> 'DRAFT'
          AND s.status IN (""" + VISIBLE_SCHEDULE_STATUSES + """
        )
          AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
          AND (:derivedStatus IS NULL OR (""" + DERIVED_STATUS + """
        ) = :derivedStatus)
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<StudentAssessmentRowInfo> findMyAssessments(
            UUID studentId,
            String assessmentKind,
            String derivedStatus,
            boolean sortDescending,
            int page,
            int size,
            Instant now) {

        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Long total = em.createQuery("SELECT COUNT(c.id) " + FROM_WHERE, Long.class)
            .setParameter("studentId", studentId)
            .setParameter("assessmentKind", assessmentKind)
            .setParameter("derivedStatus", derivedStatus)
            .setParameter("now", now)
            .getSingleResult();

        if (total == 0) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0, 0);
        }

        // Attempts with no assessment date always sort last in BOTH directions -- if reversing the direction pulled them to the top,
        // the list would open full of blank rows. `c.id` is the secondary key so that two attempts at the same time do not swap places
        // between pages: without it, pagination can repeat or skip rows.
        String direction = sortDescending ? "DESC" : "ASC";
        List<Tuple> rows = em.createQuery("""
            SELECT c.id, c.status, c.blockedAt, c.assignedPaperId,
                   e.id, e.name, e.description, e.kind, e.status, e.requiresOtp, e.maxAttempt,
                   s.id, s.startDate, s.endDate,
                   COALESCE(s.startDate, e.openAt),
                   (""" + DERIVED_STATUS + """
            )
            """ + FROM_WHERE + """
            ORDER BY COALESCE(s.startDate, e.openAt) """ + direction + """
             NULLS LAST, c.id ASC
            """, Tuple.class)
            .setParameter("studentId", studentId)
            .setParameter("assessmentKind", assessmentKind)
            .setParameter("derivedStatus", derivedStatus)
            .setParameter("now", now)
            .setFirstResult(OneBasedPaging.toFirstResult(normalizedPage, normalizedSize))
            .setMaxResults(normalizedSize)
            .getResultList();

        List<StudentAssessmentRowInfo> content = new ArrayList<StudentAssessmentRowInfo>(rows.size());
        for (Tuple row : rows) {
            content.add(new StudentAssessmentRowInfo(
                row.get(0, UUID.class),
                candidateStatusOf(row.get(1, String.class)),
                row.get(2, Instant.class),
                row.get(3, UUID.class),
                row.get(4, UUID.class),
                row.get(5, String.class),
                row.get(6, String.class),
                row.get(7, String.class),
                assessmentStatusOf(row.get(8, String.class)),
                Boolean.TRUE.equals(row.get(9, Boolean.class)),
                row.get(10, Integer.class),
                row.get(11, UUID.class),
                row.get(12, Instant.class),
                row.get(13, Instant.class),
                row.get(14, Instant.class),
                row.get(15, String.class)));
        }

        int totalPages = (int) Math.ceil(total / (double) normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    /**
     * The {@code status} column is stored as a string, so an unknown value (old data, an enum that was just renamed) must not
     * break the whole page. Returns null so the layer above handles it the "unknown" way -- the functions
     * {@code AssessmentCandidateStatus#isBlockedForEntry}/{@code isAttended} are already null-safe.
     */
    private static AssessmentCandidateStatus candidateStatusOf(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AssessmentCandidateStatus.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static AssessmentStatus assessmentStatusOf(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return AssessmentStatus.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
