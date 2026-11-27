package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.mapper.assessmentgrading.GradingResultCode;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.dto.AssessmentScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingCriterionMetaInfo;
import com.sep.vox.application.query.dto.GradingCriterionScoreInfo;
import com.sep.vox.application.query.dto.GradingAssessmentOptionInfo;
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionAssetJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.QuestionAssetMapper;
import com.sep.vox.application.query.dto.GradingTurnInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundPolicy;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.TurnType;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaAssessmentGradingQueryRepository implements AssessmentGradingQueryRepository {

    /**
     * Đơn phúc khảo còn "mở" — bài đang có đơn như vậy không được giao vòng chấm
     * khác, nếu không hai luồng cùng ghi điểm một bài (review BE-4).
     */
    private static final String OPEN_APPEAL_STATUSES = "'PENDING', 'APPROVED', 'GRADING'";

    /** Số bài chặn gửi kèm preview chốt sổ — đủ để admin nhận ra vấn đề, không dựng cả danh sách. */
    private static final int BLOCKING_SAMPLE_SIZE = 20;

    /**
     * "Bài này còn đơn phúc khảo mở" — fragment JPQL tương quan với alias {@code cr},
     * đã cân bằng ngoặc, dùng lại được ở nhiều câu.
     */
    private static final String OPEN_APPEAL_EXISTS = " EXISTS ("
        + " SELECT 1 FROM AssessmentResultAppealJpaEntity ap"
        + " WHERE ap.candidateResultId = cr.id AND ap.status IN (" + OPEN_APPEAL_STATUSES + "))";

    /**
     * Bài chặn chốt sổ: còn chờ chấm, hoặc còn đơn phúc khảo mở. Một định nghĩa dùng
     * chung cho cả câu đếm lẫn câu lấy danh sách mẫu, để hai con số không nói khác nhau.
     */
    private static final String BLOCKING_CONDITION =
        " (cr.status = :pendingReview OR " + OPEN_APPEAL_EXISTS + ")";

    /**
     * Trần số kỳ thi trả về cho dropdown lọc của giáo viên. Cùng tinh thần với
     * {@link com.sep.vox.interfaces.shared.PageArguments#MAX_PAGE_SIZE}: danh sách này không phân
     * trang nên phải có chặn trên ở đâu đó, dù một người bình thường không bao giờ chạm tới.
     */
    private static final int MAX_ASSESSMENT_OPTIONS = 200;

    /** Matches {@code AssessmentKind.CLASS_TEST}; JPQL returns the enum as a String. */
    private static final String CLASS_TEST_KIND = "CLASS_TEST";

    /** Matches {@code AssessmentKind.CENTRALIZED}. The same reason for using String as above. */
    private static final String CENTRALIZED_KIND = "CENTRALIZED";

    /** Trạng thái bài còn có thể nhận một vòng chấm — luật lấy từ domain, không chép cứng. */
    private static final List<String> ASSIGNABLE_STATUS_NAMES = GradingRoundPolicy.allAssignableStatuses()
        .stream()
        .map(status -> status.name())
        .toList();

    private static final String ASSIGNABLE_RESULT_IDS_QUERY = """
        SELECT cr.id
        FROM AssessmentCandidateResultJpaEntity cr
        JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
        JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
        WHERE e.schoolId = :schoolId
        AND e.kind = :centralizedKind
        AND cr.status IN (:resultStatuses)
        AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
        AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        AND NOT EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
        )
        AND NOT EXISTS (
            SELECT 1 FROM AssessmentResultAppealJpaEntity ap
            WHERE ap.candidateResultId = cr.id AND ap.status IN (""" + OPEN_APPEAL_STATUSES + """
            )
        )
        ORDER BY cr.id ASC
        """;

    @PersistenceContext
    private EntityManager em;

    // ---- bảng điều phối (school admin) --------------------------------------

    /**
     * Three steps, a fixed number of queries (5) no matter how many rows the page has: (1) the main pagination
     * on candidate_result, (2) load the assignments of exactly that page, (3) load the class + the
     * appeal flag.
     *
     * <p>No LEFT JOIN straight to the assignment as in the old version: an attempt now has MANY assignment
     * rows across the rounds, and joining directly would duplicate rows and break both pagination
     * and COUNT.
     */
    @Override
    public PageResult<GradingAssignmentRowInfo> searchAssignments(
            GradingAssignmentFilter filter, int page, int size) {
        Instant now = Instant.now();
        List<Tuple> rows = assignmentRowsQuery(filter, now)
            .setFirstResult(OneBasedPaging.toFirstResult(page, size))
            .setMaxResults(size)
            .getResultList();
        List<GradingAssignmentRowInfo> content = toAssignmentRows(rows, now);
        Long total = countAssignments(filter, now);

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private TypedQuery<Tuple> assignmentRowsQuery(GradingAssignmentFilter filter, Instant now) {
        return applyFilters(em.createQuery("""
            SELECT cr.id, u.fullName, e.name, cr.status, cr.totalScore, s.flagged, cr.updatedAt, cr.sessionId
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentSessionJpaEntity s ON s.id = cr.sessionId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE + """
            ORDER BY cr.updatedAt DESC, cr.id ASC
        """, Tuple.class), filter, now);
    }

    private Long countAssignments(GradingAssignmentFilter filter, Instant now) {
        return applyFilters(em.createQuery("""
            SELECT COUNT(cr)
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentSessionJpaEntity s ON s.id = cr.sessionId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE, Long.class), filter, now)
            .getSingleResult();
    }

    private record AssignmentRowLookups(
        Map<UUID, String> classNames,
        Map<UUID, DisplayAssignment> displayed,
        Set<UUID> openAppeals,
        Map<UUID, AttemptInfo> attempts
    ) {
    }

    private List<GradingAssignmentRowInfo> toAssignmentRows(List<Tuple> rows, Instant now) {
        List<UUID> candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        AssignmentRowLookups lookups = new AssignmentRowLookups(
            classNamesByCandidateResultIds(candidateResultIds),
            displayAssignmentsByResultIds(candidateResultIds),
            resultIdsWithOpenAppeal(candidateResultIds),
            attemptsByCandidateResultIds(candidateResultIds));

        List<GradingAssignmentRowInfo> content = new ArrayList<GradingAssignmentRowInfo>();
        for (Tuple row : rows) {
            content.add(toAssignmentRow(row, now, lookups));
        }
        return content;
    }

    private GradingAssignmentRowInfo toAssignmentRow(Tuple row, Instant now, AssignmentRowLookups lookups) {
        UUID candidateResultId = row.get(0, UUID.class);
        DisplayAssignment assignment = lookups.displayed().get(candidateResultId);
        AttemptInfo attempt = attemptOrSingle(lookups.attempts(), candidateResultId, row.get(7, UUID.class));
        return new GradingAssignmentRowInfo(
            candidateResultId,
            GradingResultCode.of(candidateResultId),
            row.get(1, String.class),
            lookups.classNames().get(candidateResultId),
            row.get(2, String.class),
            AssessmentCandidateResultStatus.valueOf(row.get(3, String.class)),
            row.get(4, BigDecimal.class),
            Boolean.TRUE.equals(row.get(5, Boolean.class)),
            assignment == null ? null : assignment.id(), assignment == null ? null : assignment.teacherId(),
            assignment == null ? null : assignment.teacherName(), assignment == null ? null : assignment.roundType(),
            assignment == null ? null : assignment.status(), assignment == null ? null : assignment.outcome(),
            assignment == null ? null : assignment.assignedAt(), assignment == null ? null : assignment.completedAt(),
            assignment == null ? null : assignment.deadlineAt(),
            assignment != null && assignment.isOverdue(now),
            lookups.openAppeals().contains(candidateResultId),
            attempt.sessionId(), attempt.attemptNo(), attempt.attemptCount()
        );
    }

    /**
     * Mọi bộ lọc "theo phân công" đều là EXISTS chứ không phải JOIN — câu hỏi của
     * admin là "bài nào có một phân công thoả điều kiện", và EXISTS trả lời đúng câu
     * đó mà không nhân bản dòng.
     */
    private static final String WHERE_CLAUSE = """
        WHERE e.schoolId = :schoolId
        AND cr.status <> 'DELETED'
        AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
        AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
        AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        AND (:resultStatus IS NULL OR cr.status = :resultStatus)
        AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword
             OR LOWER(CAST(cr.id AS string)) LIKE :keyword)
        AND (:teacherId IS NULL OR EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.teacherId = :teacherId))
        AND (:roundType IS NULL OR EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.roundType = :roundType))
        AND (:assignmentStatus IS NULL OR EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.status = :assignmentStatus))
        AND (:unassignedOnly IS NULL OR NOT EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id))
        AND (:overdueOnly IS NULL OR EXISTS (
            SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga
            WHERE ga.activeResultId = cr.id AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now))
        AND (:hasOpenAppeal IS NULL OR :hasOpenAppeal = (
            CASE WHEN EXISTS (
                SELECT 1 FROM AssessmentResultAppealJpaEntity ap
                WHERE ap.candidateResultId = cr.id
                AND ap.status IN (""" + OPEN_APPEAL_STATUSES + """
            )) THEN TRUE ELSE FALSE END))
        """;

    private <T> jakarta.persistence.TypedQuery<T> applyFilters(
            jakarta.persistence.TypedQuery<T> query, GradingAssignmentFilter filter, Instant now) {
        return query
            .setParameter("schoolId", filter.schoolId())
            .setParameter("assessmentKind", filter.assessmentKind())
            .setParameter("assessmentId", filter.assessmentId())
            .setParameter("scheduleId", filter.scheduleId())
            .setParameter("resultStatus", filter.resultStatus())
            .setParameter("keyword", normalizeKeyword(filter.keyword()))
            .setParameter("teacherId", filter.teacherId())
            .setParameter("roundType", filter.roundType())
            .setParameter("assignmentStatus", filter.assignmentStatus())
            // null = không lọc; TRUE = bật. Dùng Boolean nullable thay vì so `= FALSE`
            // để một câu JPQL phục vụ được cả hai trường hợp.
            .setParameter("unassignedOnly", filter.unassignedOnly() ? Boolean.TRUE : null)
            .setParameter("overdueOnly", filter.overdueOnly() ? Boolean.TRUE : null)
            .setParameter("hasOpenAppeal", filter.hasOpenAppeal())
            .setParameter("now", now);
    }

    private record DisplayAssignment(
        UUID id, UUID teacherId, String teacherName, String roundType, String status, String outcome,
        Instant assignedAt, Instant completedAt, Instant deadlineAt, boolean open
    ) {
        boolean isOverdue(Instant now) {
            return open && deadlineAt != null && deadlineAt.isBefore(now);
        }
    }

    /**
     * The assignment to display for each attempt: the open row if there is one, otherwise the most recent closed
     * row — the admin can still see who just graded this attempt in the previous round.
     */
    private Map<UUID, DisplayAssignment> displayAssignmentsByResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = findDisplayAssignmentRows(candidateResultIds);

        Map<UUID, DisplayAssignment> map = new HashMap<UUID, DisplayAssignment>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            DisplayAssignment assignment = toDisplayAssignment(row);
            DisplayAssignment existing = map.get(candidateResultId);
            // An open row always wins; among closed rows the first one (newest) wins.
            if (existing == null || (assignment.open() && !existing.open())) {
                map.put(candidateResultId, assignment);
            }
        }
        return map;
    }

    private List<Tuple> findDisplayAssignmentRows(List<UUID> candidateResultIds) {
        return em.createQuery("""
            SELECT ga.candidateResultId, ga.id, ga.teacherId, t.fullName, ga.roundType, ga.status,
                   ga.outcome, ga.assignedAt, ga.completedAt, ga.deadlineAt, ga.activeResultId
            FROM AssessmentGradingAssignmentJpaEntity ga
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE ga.candidateResultId IN (:candidateResultIds)
            ORDER BY ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
    }

    private DisplayAssignment toDisplayAssignment(Tuple row) {
        return new DisplayAssignment(
            row.get(1, UUID.class),
            row.get(2, UUID.class),
            row.get(3, String.class),
            row.get(4, String.class),
            row.get(5, String.class),
            row.get(6, String.class),
            row.get(7, Instant.class),
            row.get(8, Instant.class),
            row.get(9, Instant.class),
            row.get(10, UUID.class) != null
        );
    }

    private Set<UUID> resultIdsWithOpenAppeal(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT ap.candidateResultId FROM AssessmentResultAppealJpaEntity ap
            WHERE ap.candidateResultId IN (:candidateResultIds)
            AND ap.status IN (""" + OPEN_APPEAL_STATUSES + ")", UUID.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList());
    }

    /** Tên lớp của nhiều bài trong 1 query; giữ giá trị đầu tiên mỗi bài (tránh N+1). */
    /**
     * Student names in bulk. Only called for CLASS tests — centralized assessments keep
     * anonymous grading, and that is a fairness guarantee, not a superfluous detail.
     */
    private Map<UUID, String> studentNamesByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, u.fullName
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE cr.id IN (:candidateResultIds)
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
        Map<UUID, String> map = new HashMap<UUID, String>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, String.class));
        }
        return map;
    }

    private Map<UUID, String> classNamesByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, sc.name
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN SchoolClassUserJpaEntity scu ON scu.userId = c.studentId AND scu.isActive = true
            JOIN SchoolClassJpaEntity sc ON sc.id = scu.schoolClassId
            WHERE cr.id IN (:candidateResultIds)
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
        Map<UUID, String> map = new HashMap<UUID, String>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, String.class));
        }
        return map;
    }

    /** Lượt thi thứ mấy trên tổng mấy lượt, kèm phiên thi tương ứng. */
    record AttemptInfo(UUID sessionId, int attemptNo, int attemptCount) {

        /** Bài không tra được lượt (phiên đã xoá) vẫn phải hiện, coi như lượt duy nhất. */
        static AttemptInfo single(UUID sessionId) {
            return new AttemptInfo(sessionId, 1, 1);
        }
    }

    /**
     * The attempt number of many results in 1 query.
     *
     * <p>There is no {@code attempt_no} column in the DB: an attempt is one {@code assessment_sessions}
     * row, so the order must be derived from the {@code startedAt} of the same candidate.
     * Numbered ASCENDING to match how the student's "my assessments" screen numbers them
     * ({@code ViewMyAssessmentsUseCase}) — when both sides say "attempt 2" it must be the same attempt.
     *
     * <p>The condition {@code s.assessmentId = cr.assessmentId} is redundant as far as the data goes (every {@code assessment_candidates}
     * row already belongs to exactly one assessment) but it is kept as a guard rail:
     * counting an attempt of another assessment would give a wrong number that nobody notices is wrong.
     */
    private Map<UUID, AttemptInfo> attemptsByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = findAttemptRows(candidateResultIds);

        // Group by attempt first, then number: ORDER BY already takes care of the order, here only a running count is needed.
        Map<UUID, List<UUID>> sessionsByResult = new LinkedHashMap<UUID, List<UUID>>();
        Map<UUID, UUID> ownSessionByResult = new HashMap<UUID, UUID>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            ownSessionByResult.putIfAbsent(candidateResultId, row.get(1, UUID.class));
            sessionsByResult.computeIfAbsent(candidateResultId, ignored -> new ArrayList<>())
                .add(row.get(2, UUID.class));
        }
        return numberAttempts(sessionsByResult, ownSessionByResult);
    }

    private List<Tuple> findAttemptRows(List<UUID> candidateResultIds) {
        return em.createQuery("""
            SELECT cr.id, cr.sessionId, s.id
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN AssessmentSessionJpaEntity s ON s.candidateId = c.id AND s.assessmentId = cr.assessmentId
            WHERE cr.id IN (:candidateResultIds)
            ORDER BY cr.id ASC, s.startedAt ASC, s.id ASC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
    }

    private Map<UUID, AttemptInfo> numberAttempts(
            Map<UUID, List<UUID>> sessionsByResult, Map<UUID, UUID> ownSessionByResult) {
        Map<UUID, AttemptInfo> map = new HashMap<UUID, AttemptInfo>();
        for (Map.Entry<UUID, List<UUID>> entry : sessionsByResult.entrySet()) {
            UUID ownSessionId = ownSessionByResult.get(entry.getKey());
            int index = entry.getValue().indexOf(ownSessionId);
            map.put(entry.getKey(), index < 0
                ? AttemptInfo.single(ownSessionId)
                : new AttemptInfo(ownSessionId, index + 1, entry.getValue().size()));
        }
        return map;
    }

    /** An attempt with no attempt rows at all (bad data) must still show up instead of falling out of the table. */
    private AttemptInfo attemptOrSingle(Map<UUID, AttemptInfo> attempts, UUID candidateResultId, UUID sessionId) {
        AttemptInfo attempt = attempts.get(candidateResultId);
        return attempt == null ? AttemptInfo.single(sessionId) : attempt;
    }

    // ---- thẻ số đầu màn ----------------------------------------------------

    @Override
    public GradingStatsInfo stats(UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind) {
        Instant now = Instant.now();

        List<GradingStatsInfo.ResultStatusCount> byResultStatus =
            findResultStatusCounts(schoolId, assessmentId, scheduleId, assessmentKind);
        int total = byResultStatus.stream().mapToInt(c -> c.count()).sum();

        Tuple openRow = findOpenAssignmentCounts(schoolId, assessmentId, scheduleId, assessmentKind, now);
        int assigned = openRow.get(0, Long.class).intValue();
        int overdue = intOf(openRow.get(1, Long.class));

        Long unassigned = countUnassignedEligibleResults(schoolId, assessmentId, scheduleId, assessmentKind);
        List<GradingStatsInfo.TeacherProgress> teacherProgress =
            findTeacherProgress(schoolId, assessmentId, scheduleId, assessmentKind, now);

        return new GradingStatsInfo(
            total, byResultStatus, unassigned.intValue(), assigned, overdue, teacherProgress);
    }

    private <T> TypedQuery<T> applyStatsScope(
            TypedQuery<T> query, UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind) {
        return query
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentKind", assessmentKind)
            .setParameter("assessmentId", assessmentId)
            .setParameter("scheduleId", scheduleId);
    }

    private List<GradingStatsInfo.ResultStatusCount> findResultStatusCounts(
            UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind) {
        List<Tuple> statusRows = applyStatsScope(em.createQuery("""
            SELECT cr.status, COUNT(cr)
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND cr.status <> 'DELETED'
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY cr.status
        """, Tuple.class), schoolId, assessmentId, scheduleId, assessmentKind)
            .getResultList();

        List<GradingStatsInfo.ResultStatusCount> byResultStatus = new ArrayList<GradingStatsInfo.ResultStatusCount>();
        for (Tuple row : statusRows) {
            byResultStatus.add(new GradingStatsInfo.ResultStatusCount(
                AssessmentCandidateResultStatus.valueOf(row.get(0, String.class)), row.get(1, Long.class).intValue()));
        }
        return byResultStatus;
    }

    /** One query for all three assignment numbers: open, overdue, and (derived) unassigned. */
    private Tuple findOpenAssignmentCounts(
            UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind, Instant now) {
        return applyStatsScope(em.createQuery("""
            SELECT COUNT(ga),
                   SUM(CASE WHEN ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now THEN 1 ELSE 0 END)
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.activeResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        """, Tuple.class), schoolId, assessmentId, scheduleId, assessmentKind)
            .setParameter("now", now)
            .getSingleResult();
    }

    /**
     * "Unassigned" must count exactly what it promises: attempts that are ELIGIBLE for a grading
     * round but have no open assignment. The old way (total - assigned) also included attempts whose
     * lifecycle had ended, so the number kept growing up to the total number of attempts once the assessment
     * was fully graded -- exactly when it should drop to 0.
     */
    private Long countUnassignedEligibleResults(
            UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind) {
        return applyStatsScope(em.createQuery("""
            SELECT COUNT(cr)
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND cr.status IN (:assignableStatuses)
            AND NOT EXISTS (
                SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
            )
        """, Long.class), schoolId, assessmentId, scheduleId, assessmentKind)
            .setParameter("assignableStatuses", ASSIGNABLE_STATUS_NAMES)
            .getSingleResult();
    }

    private List<GradingStatsInfo.TeacherProgress> findTeacherProgress(
            UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind, Instant now) {
        List<Tuple> progressRows = findTeacherProgressRows(schoolId, assessmentId, scheduleId, assessmentKind, now);
        List<GradingStatsInfo.TeacherProgress> teacherProgress = new ArrayList<GradingStatsInfo.TeacherProgress>();
        for (Tuple row : progressRows) {
            teacherProgress.add(toTeacherProgress(row));
        }
        return teacherProgress;
    }

    private List<Tuple> findTeacherProgressRows(
            UUID schoolId, UUID assessmentId, UUID scheduleId, String assessmentKind, Instant now) {
        return applyStatsScope(em.createQuery("""
            SELECT ga.teacherId, t.fullName,
                   SUM(CASE WHEN ga.status = 'ASSIGNED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ga.status = 'COMPLETED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL
                            AND ga.deadlineAt < :now THEN 1 ELSE 0 END)
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY ga.teacherId, t.fullName
            ORDER BY t.fullName ASC
        """, Tuple.class), schoolId, assessmentId, scheduleId, assessmentKind)
            .setParameter("now", now)
            .getResultList();
    }

    private GradingStatsInfo.TeacherProgress toTeacherProgress(Tuple row) {
        return new GradingStatsInfo.TeacherProgress(
            row.get(0, UUID.class),
            row.get(1, String.class),
            intOf(row.get(2, Long.class)),
            intOf(row.get(3, Long.class)),
            intOf(row.get(4, Long.class))
        );
    }

    private int intOf(Long value) {
        return value == null ? 0 : value.intValue();
    }

    // ---- hàng đợi của giáo viên --------------------------------------------

    /**
     * MỘT danh sách cho cả bốn vòng. Đây chính là thứ bản mở rộng cũ phải viết một
     * read layer union 528 dòng để mô phỏng; với một bảng thì nó là một câu query.
     */
    @Override
    public PageResult<GradingTaskInfo> findTasksByTeacherId(
            UUID teacherId, String assessmentKind, String status, String roundType, int page, int size) {
        return findTasksByTeacherIdAndAssessmentId(teacherId, null, assessmentKind, status, roundType, page, size);
    }

    @Override
    public PageResult<GradingTaskInfo> findTasksByTeacherIdAndAssessmentId(
            UUID teacherId, UUID assessmentId, String assessmentKind, String status, String roundType,
            int page, int size) {
        Instant now = Instant.now();
        List<Tuple> rows = teacherTaskRowsQuery(teacherId, assessmentId, assessmentKind, status, roundType)
            .setFirstResult(OneBasedPaging.toFirstResult(page, size))
            .setMaxResults(size)
            .getResultList();
        List<GradingTaskInfo> content = toTasks(rows, now);
        Long total = countTeacherTasks(teacherId, assessmentId, assessmentKind, status, roundType);

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    /**
     * Anonymity is a rule of CENTRALIZED assessments, not a general rule: there the student data
     * must not leave this read model. A class test is the opposite --
     * the grader is the very teacher who teaches that class. So only e.kind is fetched here and then
     * identities are loaded in bulk for EXACTLY the CLASS_TEST rows (see {@link #toTasks}).
     */
    private TypedQuery<Tuple> teacherTaskRowsQuery(
            UUID teacherId, UUID assessmentId, String assessmentKind, String status, String roundType) {
        return applyTeacherTaskFilters(em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, cr.totalScore,
                   s.flagged, ga.assignedAt, ga.deadlineAt, e.kind, cr.sessionId
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentSessionJpaEntity s ON s.id = cr.sessionId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE ga.teacherId = :teacherId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
            ORDER BY ga.status ASC, ga.deadlineAt ASC NULLS LAST, ga.assignedAt DESC
        """, Tuple.class), teacherId, assessmentId, assessmentKind, status, roundType);
    }

    /**
     * The JOIN to assessments here is REQUIRED even though this statement reads no assessment column: without
     * it COUNT counts both kinds of attempt while the page returns only one kind, and the FE builds
     * empty pages that cannot be clicked.
     */
    private Long countTeacherTasks(
            UUID teacherId, UUID assessmentId, String assessmentKind, String status, String roundType) {
        return applyTeacherTaskFilters(em.createQuery("""
            SELECT COUNT(ga) FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE ga.teacherId = :teacherId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
        """, Long.class), teacherId, assessmentId, assessmentKind, status, roundType)
            .getSingleResult();
    }

    private <T> TypedQuery<T> applyTeacherTaskFilters(
            TypedQuery<T> query, UUID teacherId, UUID assessmentId, String assessmentKind, String status,
            String roundType) {
        return query
            .setParameter("teacherId", teacherId)
            .setParameter("assessmentKind", assessmentKind)
            .setParameter("assessmentId", assessmentId)
            .setParameter("status", status)
            .setParameter("roundType", roundType);
    }

    private record TaskLookups(
        Map<UUID, Integer> partCounts,
        Map<UUID, AttemptInfo> attempts,
        Map<UUID, String> studentNames,
        Map<UUID, String> classNames
    ) {
    }

    private List<GradingTaskInfo> toTasks(List<Tuple> rows, Instant now) {
        List<UUID> pageResultIds = rows.stream().map(row -> row.get(1, UUID.class)).toList();
        Map<UUID, Integer> partCounts = partCountsByCandidateResultIds(pageResultIds);
        Map<UUID, AttemptInfo> attempts = attemptsByCandidateResultIds(pageResultIds);

        // Two extra queries, run ONLY when the page contains a class-test row -- the number of
        // queries stays fixed per page, no N+1.
        List<UUID> classTestResultIds = rows.stream()
            .filter(row -> CLASS_TEST_KIND.equals(row.get(10, String.class)))
            .map(row -> row.get(1, UUID.class))
            .toList();
        TaskLookups lookups = new TaskLookups(
            partCounts,
            attempts,
            studentNamesByCandidateResultIds(classTestResultIds),
            classNamesByCandidateResultIds(classTestResultIds));

        List<GradingTaskInfo> content = new ArrayList<GradingTaskInfo>();
        for (Tuple row : rows) {
            content.add(toTask(row, now, lookups));
        }
        return content;
    }

    private GradingTaskInfo toTask(Tuple row, Instant now, TaskLookups lookups) {
        UUID candidateResultId = row.get(1, UUID.class);
        String assignmentStatus = row.get(4, String.class);
        Instant deadlineAt = row.get(9, Instant.class);
        AttemptInfo attempt = attemptOrSingle(lookups.attempts(), candidateResultId, row.get(11, UUID.class));
        return new GradingTaskInfo(
            row.get(0, UUID.class),
            candidateResultId,
            GradingResultCode.of(candidateResultId),
            row.get(2, String.class),
            lookups.partCounts().getOrDefault(candidateResultId, 0),
            row.get(3, String.class),
            assignmentStatus,
            AssessmentCandidateResultStatus.valueOf(row.get(5, String.class)),
            row.get(6, BigDecimal.class),
            Boolean.TRUE.equals(row.get(7, Boolean.class)),
            row.get(8, Instant.class),
            deadlineAt,
            isOverdue(assignmentStatus, deadlineAt, now),
            lookups.studentNames().get(candidateResultId),
            lookups.classNames().get(candidateResultId),
            attempt.sessionId(), attempt.attemptNo(), attempt.attemptCount()
        );
    }

    /**
     * Derive the assessment scope FROM the set of assignments, not from the assessments table: a grading teacher is not
     * a member of a centralized assessment, so every normal read path over assessments is closed to
     * them and asking there only returns an empty list.
     *
     * <p>COMPLETED assignments are counted too — the filter must still be able to select an assessment that was just fully graded,
     * otherwise submitting the last attempt makes the whole assessment vanish from the dropdown right under their hand.
     *
     * <p>Join through {@code ga.candidateResultId}, NOT {@code ga.activeResultId}:
     * the latter is null on every closed row, and using it would lose the whole "has graded before" half.
     */
    @Override
    public List<GradingAssessmentOptionInfo> findAssessmentsWithTasksByTeacherId(UUID teacherId, String assessmentKind) {
        List<Tuple> rows = findAssessmentOptionRows(teacherId, assessmentKind);

        List<GradingAssessmentOptionInfo> options = new ArrayList<GradingAssessmentOptionInfo>();
        for (Tuple row : rows) {
            options.add(new GradingAssessmentOptionInfo(
                row.get(0, UUID.class),
                row.get(1, String.class),
                row.get(2, String.class),
                longOf(row.get(3, Long.class)),
                longOf(row.get(4, Long.class))
            ));
        }
        return options;
    }

    /**
     * GROUP BY instead of SELECT DISTINCT: it yields the same distinct set of assessments, but also gets
     * the two counts in one pass. With DISTINCT every row would need its own count query
     * -- a genuine N+1 for a single dropdown.
     */
    private List<Tuple> findAssessmentOptionRows(UUID teacherId, String assessmentKind) {
        return em.createQuery("""
            SELECT e.id, e.code, e.name, COUNT(ga),
                   SUM(CASE WHEN ga.status = :assignedStatus THEN 1 ELSE 0 END)
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE ga.teacherId = :teacherId
            AND (:assessmentKind IS NULL OR e.kind = :assessmentKind)
            GROUP BY e.id, e.code, e.name
            ORDER BY MAX(ga.assignedAt) DESC, e.name ASC
        """, Tuple.class)
            .setParameter("teacherId", teacherId)
            .setParameter("assessmentKind", assessmentKind)
            .setParameter("assignedStatus", GradingAssignmentStatus.ASSIGNED.name())
            .setMaxResults(MAX_ASSESSMENT_OPTIONS)
            .getResultList();
    }

    /** {@code SUM(CASE …)} trả null cho nhóm rỗng — cùng lý do với {@code intOf}. */
    private long longOf(Long value) {
        return value == null ? 0L : value;
    }

    private boolean isOverdue(String assignmentStatus, Instant deadlineAt, Instant now) {
        return GradingAssignmentStatus.ASSIGNED.name().equals(assignmentStatus)
            && deadlineAt != null && deadlineAt.isBefore(now);
    }

    /** The number of parts to grade for many attempts in 1 query — not counted row by row. */
    private Map<UUID, Integer> partCountsByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, COUNT(r)
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            WHERE cr.id IN (:candidateResultIds)
            GROUP BY cr.id
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
        Map<UUID, Integer> map = new HashMap<UUID, Integer>();
        for (Tuple row : rows) {
            map.put(row.get(0, UUID.class), row.get(1, Long.class).intValue());
        }
        return map;
    }

    // ---- màn chấm ----------------------------------------------------------

    @Override
    public Optional<GradingTaskDetailInfo> findTaskDetail(UUID assignmentId, UUID teacherId) {
        return findTaskDetailRows(assignmentId, teacherId).stream().findFirst().map(this::toTaskDetail);
    }

    /**
     * This query runs ALONE and before every other query -- it is the authorization
     * gate. No assessment-slot query may run before it has returned a row.
     */
    private List<Tuple> findTaskDetailRows(UUID assignmentId, UUID teacherId) {
        return em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, s.flagged, s.flagReason,
                   cr.totalScore, ga.scoreBefore, ga.deadlineAt, cr.rubricVersionId, cr.sessionId, ap.reason,
                   e.kind
            FROM AssessmentGradingAssignmentJpaEntity ga
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN AssessmentSessionJpaEntity s ON s.id = cr.sessionId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            LEFT JOIN AssessmentResultAppealJpaEntity ap ON ap.id = ga.appealId
            WHERE ga.id = :assignmentId AND ga.teacherId = :teacherId
        """, Tuple.class)
            .setParameter("assignmentId", assignmentId)
            .setParameter("teacherId", teacherId)
            .getResultList();
    }

    /**
     * A centralized assessment keeps its anonymous grading -- the student and class lookups do not run for it.
     */
    private GradingTaskDetailInfo toTaskDetail(Tuple row) {
        UUID candidateResultId = row.get(1, UUID.class);
        String roundType = row.get(3, String.class);
        String assignmentStatus = row.get(4, String.class);
        AssessmentCandidateResultStatus resultStatus = AssessmentCandidateResultStatus.valueOf(row.get(5, String.class));
        Instant deadlineAt = row.get(10, Instant.class);
        boolean isClassTest = CLASS_TEST_KIND.equals(row.get(14, String.class));
        UUID sessionId = row.get(12, UUID.class);
        AttemptInfo attempt = attemptOrSingle(
            attemptsByCandidateResultIds(List.of(candidateResultId)), candidateResultId, sessionId);
        boolean editable = isEditable(roundType, assignmentStatus, resultStatus);
        return new GradingTaskDetailInfo(
            row.get(0, UUID.class), candidateResultId, GradingResultCode.of(candidateResultId),
            row.get(2, String.class), roundType, assignmentStatus, resultStatus,
            Boolean.TRUE.equals(row.get(6, Boolean.class)), row.get(7, String.class),
            row.get(8, BigDecimal.class), row.get(9, BigDecimal.class), deadlineAt,
            isOverdue(assignmentStatus, deadlineAt, Instant.now()), editable,
            editable ? allowedOutcomeNames(roundType) : List.<String>of(),
            row.get(13, String.class), taskItems(sessionId), criteria(row.get(11, UUID.class)),
            isClassTest ? studentNameOf(candidateResultId) : null,
            isClassTest ? classNameOf(candidateResultId) : null,
            attempt.sessionId(), attempt.attemptNo(), attempt.attemptCount()
        );
    }

    /**
     * The read-only flag and the button list are decided by the BE, the FE does not derive them: the round x action
     * rules live in GradingRoundPolicy, and re-deriving them in the FE means the two sides will drift apart sooner or later.
     */
    private boolean isEditable(
            String roundType, String assignmentStatus, AssessmentCandidateResultStatus resultStatus) {
        return !GradingAssignmentStatus.COMPLETED.name().equals(assignmentStatus)
            && GradingRoundPolicy.isAssignable(GradingRoundType.valueOf(roundType), resultStatus);
    }

    private List<String> allowedOutcomeNames(String roundType) {
        return GradingRoundPolicy.allowedOutcomes(GradingRoundType.valueOf(roundType))
            .stream().map(outcome -> outcome.name()).sorted().toList();
    }

    private String studentNameOf(UUID candidateResultId) {
        return studentNamesByCandidateResultIds(List.of(candidateResultId)).get(candidateResultId);
    }

    private String classNameOf(UUID candidateResultId) {
        return classNamesByCandidateResultIds(List.of(candidateResultId)).get(candidateResultId);
    }

    /**
     * The assessment parts of the attempt with the data needed for grading. The number of queries is fixed (4) no matter how
     * many parts the attempt has.
     *
     * <p>The two different evaluation lookups are intentional: the reference score/comment comes from the
     * evaluation currently in effect, while the spoken turns must come from the AI version because only the AI version
     * produces turns.
     */
    private List<GradingTaskItemInfo> taskItems(UUID sessionId) {
        List<Tuple> rows = findTaskItemRows(sessionId);
        if (rows.isEmpty()) {
            return List.of();
        }
        TaskItemLookups lookups = loadTaskItemLookups(rows);

        // Question number within the section: rows are already sorted by (sec.order, pi.order), so a running
        // count per sectionId is enough. A question that belongs to no section becomes question 1.
        Map<UUID, Integer> seenPerSection = new HashMap<UUID, Integer>();
        List<GradingTaskItemInfo> result = new ArrayList<GradingTaskItemInfo>();
        for (Tuple row : rows) {
            int orderInSection = nextOrderInSection(seenPerSection, row.get(3, UUID.class));
            result.add(toTaskItem(row, orderInSection, lookups));
        }
        return result;
    }

    private List<Tuple> findTaskItemRows(UUID sessionId) {
        return em.createQuery("""
            SELECT r.id, r.paperItemId, sec.title, pi.sectionId, q.questionText, pi.questionId
            FROM AssessmentItemResponseJpaEntity r
            LEFT JOIN AssessmentPaperItemJpaEntity pi ON pi.id = r.paperItemId
            LEFT JOIN AssessmentPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            LEFT JOIN QuestionJpaEntity q ON q.id = pi.questionId
            WHERE r.sessionId = :sessionId
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("sessionId", sessionId)
            .getResultList();
    }

    private record TaskItemLookups(
        Map<UUID, CurrentEvaluation> currentEvaluations,
        Map<UUID, AiEvaluation> aiEvaluations,
        Map<UUID, List<GradingCriterionScoreInfo>> scoresByEvaluation,
        Map<UUID, List<GradingTurnInfo>> turnsByEvaluation,
        Map<UUID, List<GradingTurnInfo>> fallbackTurnsByResponse,
        Map<UUID, QuestionAssetDto> assetsByQuestion
    ) {
    }

    private TaskItemLookups loadTaskItemLookups(List<Tuple> rows) {
        List<UUID> responseIds = rows.stream().map(row -> row.get(0, UUID.class)).filter(Objects::nonNull).toList();
        Map<UUID, CurrentEvaluation> currentEvaluations = currentEvaluationsByResponseIds(responseIds);
        Map<UUID, AiEvaluation> aiEvaluations = aiEvaluationsByResponseIds(responseIds);
        Map<UUID, List<GradingCriterionScoreInfo>> scoresByEvaluation =
            criterionScoresByEvaluationIds(scoreEvaluationIds(currentEvaluations, aiEvaluations));
        Map<UUID, List<GradingTurnInfo>> turnsByEvaluation = turnsByEvaluationIds(
            aiEvaluations.values().stream().map(evaluation -> evaluation.id()).toList());
        Map<UUID, List<GradingTurnInfo>> fallbackTurnsByResponse = turnsByResponseIds(
            responseIdsWithoutAiTurns(responseIds, aiEvaluations, turnsByEvaluation));
        Map<UUID, QuestionAssetDto> assetsByQuestion = questionAssetsByQuestionIds(
            rows.stream().map(row -> row.get(5, UUID.class)).filter(Objects::nonNull).distinct().toList());
        return new TaskItemLookups(
            currentEvaluations, aiEvaluations, scoresByEvaluation, turnsByEvaluation,
            fallbackTurnsByResponse, assetsByQuestion);
    }

    /**
     * One query for BOTH versions: two separate calls would only cost an extra round-trip and
     * would still have to group by evaluationId in exactly the same way.
     */
    private List<UUID> scoreEvaluationIds(
            Map<UUID, CurrentEvaluation> currentEvaluations, Map<UUID, AiEvaluation> aiEvaluations) {
        List<UUID> scoreEvaluationIds = new ArrayList<UUID>();
        currentEvaluations.values().forEach(evaluation -> scoreEvaluationIds.add(evaluation.id()));
        aiEvaluations.values().forEach(evaluation -> scoreEvaluationIds.add(evaluation.id()));
        return scoreEvaluationIds;
    }

    /**
     * A question that has NEVER been graded by the AI has no row in assessment_item_evaluation_turns,
     * so the grading screen shows up COMPLETELY EMPTY -- no transcript, no audio, not even the question.
     *
     * <p>Measured on production 2026-08-16, session 01a00e64: the attempt was force-ended during the
     * assessment so the AI had not graded any question yet. Result: 0 evaluation, 0 evaluation_turn --
     * BUT 2 responses and 3 response_turns were intact, with both the transcript and the audio_url pointing to
     * S3. The evidence was still there, the grading screen just did not look where it lives. The teacher opened it
     * and saw a blank page with no way to grade by hand.
     *
     * <p>Only queried when a question is REALLY missing: a normally graded attempt keeps exactly the 4 queries
     * stated in the comment at the top of {@code taskItems}, with no extra round-trip.
     */
    private List<UUID> responseIdsWithoutAiTurns(
            List<UUID> responseIds,
            Map<UUID, AiEvaluation> aiEvaluations,
            Map<UUID, List<GradingTurnInfo>> turnsByEvaluation) {
        return responseIds.stream()
            .filter(responseId -> {
                AiEvaluation ai = aiEvaluations.get(responseId);
                return ai == null || turnsByEvaluation.getOrDefault(ai.id(), List.of()).isEmpty();
            })
            .toList();
    }

    private int nextOrderInSection(Map<UUID, Integer> seenPerSection, UUID sectionId) {
        if (sectionId == null) {
            return 1;
        }
        Integer seen = seenPerSection.get(sectionId);
        int orderInSection = seen == null ? 1 : seen + 1;
        seenPerSection.put(sectionId, orderInSection);
        return orderInSection;
    }

    /**
     * The LEFT JOIN to the question sits inside the existing item query: no extra round-trip,
     * keeping the "fixed number of queries" stated at the top of {@code taskItems}.
     */
    private GradingTaskItemInfo toTaskItem(Tuple row, int orderInSection, TaskItemLookups lookups) {
        UUID responseId = row.get(0, UUID.class);
        CurrentEvaluation current = lookups.currentEvaluations().get(responseId);
        AiEvaluation ai = lookups.aiEvaluations().get(responseId);
        return new GradingTaskItemInfo(
            row.get(1, UUID.class),
            responseId,
            row.get(2, String.class),
            row.get(3, UUID.class),
            orderInSection,
            row.get(4, String.class),
            lookups.assetsByQuestion().get(row.get(5, UUID.class)),
            current == null ? null : current.itemScore(), current == null ? null : current.feedbackSummary(),
            current == null ? List.of() : lookups.scoresByEvaluation().getOrDefault(current.id(), List.of()),
            turnsForItem(responseId, ai, lookups.turnsByEvaluation(), lookups.fallbackTurnsByResponse()),
            ai == null ? List.of() : lookups.scoresByEvaluation().getOrDefault(ai.id(), List.of()),
            ai == null ? null : ai.overallConfidence(), ai == null ? null : ai.feedbackSummary(),
            ai != null && ai.requiresHumanReview(), ai == null ? null : ai.reviewReasonCode(),
            ai != null && ai.markedInvalid(), ai != null && ai.requiresRetake(),
            ai == null ? null : ai.signals(), ai == null ? null : ai.validityJson()
        );
    }

    /**
     * The assets of each question, gathered in ONE query and then mapped by questionId.
     *
     * <p>Deliberately not a LEFT JOIN straight into the {@code taskItems} query: {@code question_assets} is a
     * one-to-many relation at the schema level (only {@code CreateQuestionAssetUseCase} blocks a second one), so a
     * question that happens to have two assets in old data would duplicate the WHOLE QUESTION ROW — the teacher sees question 2
     * appear twice on the grading screen. One extra round-trip is much cheaper than that bug.
     *
     * <p>For a question with several assets the one with the smallest {@code order} is taken, the same rule as
     * {@code GetAssessmentSessionPaperUseCase} ({@code findByQuestionId(...).findFirst()}) — so the grading screen
     * and the assessment screen never look at two different assets of the same question.
     */
    private Map<UUID, QuestionAssetDto> questionAssetsByQuestionIds(List<UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }

        List<QuestionAssetJpaEntity> assets = em.createQuery("""
            SELECT a FROM QuestionAssetJpaEntity a
            WHERE a.questionId IN :questionIds
            ORDER BY a.order ASC
        """, QuestionAssetJpaEntity.class)
            .setParameter("questionIds", questionIds)
            .getResultList();

        Map<UUID, QuestionAssetDto> byQuestion = new HashMap<UUID, QuestionAssetDto>();
        for (QuestionAssetJpaEntity asset : assets) {
            byQuestion.putIfAbsent(
                asset.getQuestionId(),
                QuestionAssetDtoMapper.toDto(QuestionAssetMapper.toDomain(asset)));
        }
        return byQuestion;
    }

    private record CurrentEvaluation(UUID id, BigDecimal itemScore, String feedbackSummary) {
    }

    private record AiEvaluation(
        UUID id,
        BigDecimal overallConfidence,
        String feedbackSummary,
        boolean requiresHumanReview,
        String reviewReasonCode,
        boolean markedInvalid,
        boolean requiresRetake,
        String signals,
        String validityJson
    ) {
    }

    /**
     * The evaluation currently in effect for each response — the reference point the teacher compares against. The first time
     * it is the AI AUTO_GRADED version; after a manual grading it is the HUMAN FINALIZED version.
     *
     * <p>The filter {@code status IN ('AUTO_GRADED','FINALIZED')} is mandatory in EVERY
     * "latest evaluation" query — see {@code AI/shared/context.md}.
     *
     * <p>Order then keep the first row, do not use MAX(evaluatedAt): two versions with the same timestamp
     * would make an equality comparison return both.
     */
    private Map<UUID, CurrentEvaluation> currentEvaluationsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ev.responseId, ev.id, ev.itemScore, ev.feedbackSummary
            FROM AssessmentItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, CurrentEvaluation> map = new LinkedHashMap<UUID, CurrentEvaluation>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), new CurrentEvaluation(
                row.get(1, UUID.class), row.get(2, BigDecimal.class), row.get(3, String.class)));
        }
        return map;
    }

    /**
     * The AI version of each response — the ONLY source of spoken turns (audio/transcript) and of
     * all AI evidence (signals, validity, word feedback), because a manual evaluation produces no
     * turns and does not have these signals either. After the teacher submits, the AI version carries
     * SUPERSEDED, so this query deliberately does not filter by status.
     */
    private Map<UUID, AiEvaluation> aiEvaluationsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = findAiEvaluationRows(responseIds);
        Map<UUID, AiEvaluation> map = new LinkedHashMap<UUID, AiEvaluation>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), toAiEvaluation(row));
        }
        return map;
    }

    private List<Tuple> findAiEvaluationRows(List<UUID> responseIds) {
        return em.createQuery("""
            SELECT ev.responseId, ev.id, ev.overallConfidence, ev.feedbackSummary,
                   ev.requiresHumanReview, ev.reviewReasonCode, ev.markedInvalid,
                   ev.requiresRetake, ev.signals, ev.validityJson
            FROM AssessmentItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
    }

    private AiEvaluation toAiEvaluation(Tuple row) {
        return new AiEvaluation(
            row.get(1, UUID.class),
            row.get(2, BigDecimal.class),
            row.get(3, String.class),
            Boolean.TRUE.equals(row.get(4, Boolean.class)),
            row.get(5, String.class),
            Boolean.TRUE.equals(row.get(6, Boolean.class)),
            Boolean.TRUE.equals(row.get(7, Boolean.class)),
            row.get(8, String.class),
            row.get(9, String.class)
        );
    }

    /** Criterion scores of many evaluations in 1 query, grouped by evaluationId. */
    private Map<UUID, List<GradingCriterionScoreInfo>> criterionScoresByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cs.evaluationId, cs.rubricCriterionId, rc.code, rc.name, cs.finalScore, cs.rationale
            FROM AssessmentItemCriterionScoreJpaEntity cs
            JOIN RubricCriterionJpaEntity rc ON rc.id = cs.rubricCriterionId
            WHERE cs.evaluationId IN (:evaluationIds)
            ORDER BY rc.order ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        Map<UUID, List<GradingCriterionScoreInfo>> map = new LinkedHashMap<UUID, List<GradingCriterionScoreInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(toCriterionScore(row));
        }
        return map;
    }

    private GradingCriterionScoreInfo toCriterionScore(Tuple row) {
        return new GradingCriterionScoreInfo(
            row.get(1, UUID.class),
            row.get(2, String.class),
            row.get(3, String.class),
            row.get(4, BigDecimal.class),
            row.get(5, String.class)
        );
    }

    /**
     * The spoken turns of one question: prefer the AI version, and if it is missing fall back to the candidate's ORIGINAL turns.
     *
     * <p>Do not mix the two sources. The AI version has three things the original table does not --
     * {@code asrConfidence}, {@code pronunciationOverall}, {@code wordFeedback} -- so a graded attempt
     * must take the AI version in full, exactly as before this function existed. For an ungraded attempt those three columns
     * do not exist yet, so leaving them empty is correct, not a loss.
     */
    private List<GradingTurnInfo> turnsForItem(
            UUID responseId,
            AiEvaluation ai,
            Map<UUID, List<GradingTurnInfo>> turnsByEvaluation,
            Map<UUID, List<GradingTurnInfo>> fallbackTurnsByResponse) {
        if (ai != null) {
            List<GradingTurnInfo> aiTurns = turnsByEvaluation.getOrDefault(ai.id(), List.of());
            if (!aiTurns.isEmpty()) {
                return aiTurns;
            }
        }
        return fallbackTurnsByResponse.getOrDefault(responseId, List.of());
    }

    /**
     * The candidate's ORIGINAL spoken turns, read straight from {@code assessment_item_response_turns}.
     *
     * <p>This table is raw evidence -- it belongs to the ANSWER, not to any grading pass --
     * so it is still intact even if the AI never graded, or if the attempt was invalidated midway.
     *
     * <p>The three AI-only columns return {@code null}: the original table does not have them, and inventing a
     * substitute value is worse than leaving them empty -- the UI can tell "not graded yet" from "graded as 0".
     */
    private Map<UUID, List<GradingTurnInfo>> turnsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT t.assessmentItemResponseId, t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl,
                   t.transcript, t.durationSeconds, t.wordCount
            FROM AssessmentItemResponseTurnJpaEntity t
            WHERE t.assessmentItemResponseId IN (:responseIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, List<GradingTurnInfo>> map = new LinkedHashMap<UUID, List<GradingTurnInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(toOriginalTurn(row));
        }
        return map;
    }

    private GradingTurnInfo toOriginalTurn(Tuple row) {
        return new GradingTurnInfo(
            row.get(1, UUID.class),
            row.get(2, Integer.class),
            TurnType.valueOf(row.get(3, String.class)),
            row.get(4, String.class),
            row.get(5, String.class),
            row.get(6, String.class),
            row.get(7, Integer.class),
            row.get(8, Integer.class),
            null,
            null,
            null
        );
    }

    /** Spoken turns of many evaluations in 1 query, grouped by evaluationId. */
    private Map<UUID, List<GradingTurnInfo>> turnsByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT t.evaluationId, t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl,
                   t.transcript, t.durationSeconds, t.wordCount, t.asrConfidence,
                   t.pronunciationOverall, t.wordFeedback
            FROM AssessmentItemEvaluationTurnJpaEntity t
            WHERE t.evaluationId IN (:evaluationIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        Map<UUID, List<GradingTurnInfo>> map = new LinkedHashMap<UUID, List<GradingTurnInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(toEvaluationTurn(row));
        }
        return map;
    }

    private GradingTurnInfo toEvaluationTurn(Tuple row) {
        return new GradingTurnInfo(
            row.get(1, UUID.class),
            row.get(2, Integer.class),
            TurnType.valueOf(row.get(3, String.class)),
            row.get(4, String.class),
            row.get(5, String.class),
            row.get(6, String.class),
            row.get(7, Integer.class),
            row.get(8, Integer.class),
            row.get(9, Double.class),
            row.get(10, String.class),
            row.get(11, String.class)
        );
    }

    /** Tiêu chí kèm min/max/weight — FE dựng ô nhập theo dải điểm thật của rubric. */
    private List<GradingCriterionMetaInfo> criteria(UUID rubricVersionId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.GradingCriterionMetaInfo(
                rc.id, rc.code, rc.name, rc.description, rc.minScore, rc.maxScore, rc.weight, rc.isRequired)
            FROM RubricCriterionJpaEntity rc
            WHERE rc.rubricVersionId = :rubricVersionId
            ORDER BY rc.order ASC
        """, GradingCriterionMetaInfo.class)
            .setParameter("rubricVersionId", rubricVersionId)
            .getResultList();
    }

    // ---- picker phân công --------------------------------------------------

    @Override
    public List<AssignableTeacherInfo> findAssignableTeachers(UUID schoolId, String keyword) {
        List<Tuple> rows = findActiveTeacherRows(schoolId, normalizeKeyword(keyword));

        List<UUID> teacherIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, Long> loadByTeacher = assignedLoadByTeacherIds(teacherIds);

        List<AssignableTeacherInfo> result = new ArrayList<AssignableTeacherInfo>();
        for (Tuple row : rows) {
            UUID teacherId = row.get(0, UUID.class);
            result.add(new AssignableTeacherInfo(
                teacherId, row.get(1, String.class), loadByTeacher.getOrDefault(teacherId, 0L)));
        }
        return result;
    }

    private List<Tuple> findActiveTeacherRows(UUID schoolId, String normalizedKeyword) {
        return em.createQuery("""
            SELECT DISTINCT u.id, u.fullName
            FROM SchoolUserJpaEntity su
            JOIN UserJpaEntity u ON u.id = su.userId
            JOIN UserRoleJpaEntity ur ON ur.userId = u.id
            JOIN RoleJpaEntity ro ON ro.id = ur.roleId
            WHERE su.schoolId = :schoolId
            AND ro.code = 'TEACHER'
            AND u.status = 'ACTIVE'
            AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword)
            ORDER BY u.fullName ASC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("keyword", normalizedKeyword)
            .getResultList();
    }

    /**
     * Lọc tập userId xuống những giáo viên ACTIVE thuộc trường, trong 1 query.
     * Thay cho {@code isTeacherOfSchool} gọi lặp từng người khi gán tay / auto-assign.
     */
    @Override
    public Set<UUID> findTeacherIdsInSchool(UUID schoolId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT u.id
            FROM SchoolUserJpaEntity su
            JOIN UserJpaEntity u ON u.id = su.userId
            JOIN UserRoleJpaEntity ur ON ur.userId = u.id
            JOIN RoleJpaEntity ro ON ro.id = ur.roleId
            WHERE su.schoolId = :schoolId
            AND ro.code = 'TEACHER'
            AND u.status = 'ACTIVE'
            AND u.id IN (:userIds)
        """, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("userIds", userIds)
            .getResultList());
    }

    /**
     * Load = the number of OPEN assignments, counted through {@code active_result_id} so it is automatically right for all
     * four rounds: a teacher who both does first-round grading and post-review has a load equal to the sum of both.
     */
    @Override
    public Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ga.teacherId, COUNT(ga)
            FROM AssessmentGradingAssignmentJpaEntity ga
            WHERE ga.teacherId IN (:teacherIds) AND ga.activeResultId IS NOT NULL
            GROUP BY ga.teacherId
        """, Tuple.class)
            .setParameter("teacherIds", teacherIds)
            .getResultList();
        Map<UUID, Long> map = new HashMap<UUID, Long>();
        for (Tuple row : rows) {
            map.put(row.get(0, UUID.class), row.get(1, Long.class));
        }
        return map;
    }

    // ---- auto-assign -------------------------------------------------------

    /**
     * NOT EXISTS on active_result_id instead of LEFT JOIN ... IS NULL: the unique index sits on exactly that
     * column, and this keeps repeated auto-assign runs safe.
     *
     * <p>An attempt with an open appeal is also excluded -- the APPEAL round is assigned by the appeal flow, not
     * by auto-assign, and assigning another round at that time would leave two people scoring the same attempt
     * (review BE-4).
     *
     * <p>Class tests are excluded by a hard-coded rule here rather than a parameter: auto-assign is a school
     * action, and the school does not assign class-test grading.
     */
    @Override
    public List<UUID> findAssignableResultIds(
            UUID schoolId, UUID assessmentId, UUID scheduleId, Collection<String> resultStatuses) {
        if (resultStatuses == null || resultStatuses.isEmpty()) {
            return List.of();
        }
        return em.createQuery(ASSIGNABLE_RESULT_IDS_QUERY, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("centralizedKind", CENTRALIZED_KIND)
            .setParameter("resultStatuses", resultStatuses)
            .setParameter("assessmentId", assessmentId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();
    }

    /**
     * The LOWEST confidence among the parts of the attempt, with the score and the passing threshold. Min is taken because
     * one weak AI part is enough to make the whole attempt worth a look — not the average, so that
     * easy parts do not pull it up.
     */
    @Override
    public List<GradingRiskInfo> findRiskInfos(Collection<UUID> candidateResultIds) {
        if (candidateResultIds == null || candidateResultIds.isEmpty()) {
            return List.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, MIN(ev.overallConfidence), cr.totalScore, pol.passingScore
            FROM AssessmentCandidateResultJpaEntity cr
            LEFT JOIN AssessmentPolicyJpaEntity pol ON pol.id = cr.assessmentPolicyId
            LEFT JOIN AssessmentItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            LEFT JOIN AssessmentItemEvaluationJpaEntity ev ON ev.responseId = r.id
                AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
            WHERE cr.id IN (:candidateResultIds)
            GROUP BY cr.id, cr.totalScore, pol.passingScore
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();

        List<GradingRiskInfo> result = new ArrayList<GradingRiskInfo>();
        for (Tuple row : rows) {
            result.add(toRiskInfo(row));
        }
        return result;
    }

    private GradingRiskInfo toRiskInfo(Tuple row) {
        return new GradingRiskInfo(
            row.get(0, UUID.class),
            row.get(1, BigDecimal.class),
            row.get(2, BigDecimal.class),
            row.get(3, BigDecimal.class)
        );
    }

    /**
     * Ai đã từng GHI ĐIỂM tay cho bài này. Người chấm tay được lưu ở
     * {@code reviewerId} của evaluation engine HUMAN (cột {@code gradedByModel} dành
     * cho tên model AI, luồng chấm tay ghi hằng "HUMAN" vào đó).
     *
     * <p>Người chỉ xác nhận điểm cũ (UPHOLD) không sinh evaluation nào nên không nằm
     * trong tập này — họ vẫn được chấm phúc khảo.
     */
    @Override
    public Set<UUID> findTeacherIdsWithHumanEvaluation(UUID candidateResultId) {
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT ev.reviewerId
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            JOIN AssessmentItemEvaluationJpaEntity ev ON ev.responseId = r.id
            WHERE cr.id = :candidateResultId
            AND ev.engineType = 'HUMAN'
            AND ev.reviewerId IS NOT NULL
        """, UUID.class)
            .setParameter("candidateResultId", candidateResultId)
            .getResultList());
    }

    // ---- chốt sổ & xuất bảng điểm ------------------------------------------

    /**
     * The numbers that block finalization: attempts waiting for grading that nobody has taken, attempts waiting for grading that
     * someone is holding, unfinished appeals, and invalidated attempts.
     *
     * <p>Two queries, both aggregates: one {@code SUM(CASE WHEN …)} statement for the
     * numbers, and one {@code LIMIT 20} statement for the list of blocking attempts used as examples. The previous version pulled EVERY
     * row of the assessment into Java and counted with a loop — acceptable at the current scale,
     * but this is a pure aggregate so there is no reason for the data to leave the DB.
     */
    @Override
    public BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID assessmentId) {
        Tuple counts = findBulkFinalizeCounts(schoolId, assessmentId);

        int total = intOf(counts.get(0, Long.class));
        int blocked = intOf(counts.get(5, Long.class));

        List<UUID> blockingResultIds = blocked == 0
            ? List.<UUID>of() : findBlockingResultIds(schoolId, assessmentId);

        return new BulkFinalizePreviewInfo(
            total,
            total - blocked,
            intOf(counts.get(1, Long.class)),
            intOf(counts.get(2, Long.class)),
            intOf(counts.get(3, Long.class)),
            intOf(counts.get(4, Long.class)),
            List.copyOf(blockingResultIds));
    }

    private Tuple findBulkFinalizeCounts(UUID schoolId, UUID assessmentId) {
        return em.createQuery("""
            SELECT COUNT(cr),
                   SUM(CASE WHEN cr.status = :pendingReview AND NOT EXISTS (
                       SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
                   ) THEN 1 ELSE 0 END),
                   SUM(CASE WHEN cr.status = :pendingReview AND EXISTS (
                       SELECT 1 FROM AssessmentGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
                   ) THEN 1 ELSE 0 END),
                   SUM(CASE WHEN """ + OPEN_APPEAL_EXISTS + """
                        THEN 1 ELSE 0 END),
                   SUM(CASE WHEN cr.status = :invalid THEN 1 ELSE 0 END),
                   SUM(CASE WHEN """ + BLOCKING_CONDITION + """
                        THEN 1 ELSE 0 END)
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE e.schoolId = :schoolId AND cr.assessmentId = :assessmentId AND cr.status <> 'DELETED'
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("pendingReview", AssessmentCandidateResultStatus.PENDING_REVIEW.name())
            .setParameter("invalid", AssessmentCandidateResultStatus.INVALID.name())
            .getSingleResult();
    }

    /**
     * Sample list: the UI only needs a few examples so the admin recognizes the problem, the total count is
     * already in the count fields of {@link #findBulkFinalizeCounts}.
     */
    private List<UUID> findBlockingResultIds(UUID schoolId, UUID assessmentId) {
        return em.createQuery("""
            SELECT cr.id
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE e.schoolId = :schoolId AND cr.assessmentId = :assessmentId
            AND """ + BLOCKING_CONDITION + """
            ORDER BY cr.id ASC
        """, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("pendingReview", AssessmentCandidateResultStatus.PENDING_REVIEW.name())
            .setMaxResults(BLOCKING_SAMPLE_SIZE)
            .getResultList();
    }

    /**
     * The score sheet for file export. Two steps: fetch the main rows, then load the most recent grading round in
     * bulk — no query repeated per student.
     *
     * <p>The "assessment shift" column is the schedule's {@code startDate}, not a name:
     * {@code assessment_schedules} has no name column, a shift is identified by its start time
     * (the previous version selected {@code sch.name}, so this statement threw {@code UnknownPathException}
     * on every export).
     *
     * <p>Reuses {@link #WHERE_CLAUSE} and {@link #applyFilters} of the dispatch table instead of
     * writing a separate WHERE clause. The previous version wrote its own and FORGOT the {@code e.kind} condition
     * — every other statement in this class has it — so an export from the class-test screen still pulled in
     * the scores of centralized assessments of the same school. With one shared predicate the exported file
     * cannot drift from the table the user is looking at.
     *
     * <p>The aliases that {@code WHERE_CLAUSE} needs ({@code cr}, {@code e}, {@code c},
     * {@code u}) are all already in the FROM below; it does not touch {@code s}
     * ({@code AssessmentSessionJpaEntity}) so no extra join is needed.
     */
    @Override
    public List<AssessmentScoreRowInfo> findScoreRows(GradingAssignmentFilter filter) {
        List<Tuple> rows = findScoreSheetRows(filter);

        List<UUID> candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, String> classNames = classNamesByCandidateResultIds(candidateResultIds);
        Map<UUID, LastRound> lastRounds = lastCompletedRoundByResultIds(candidateResultIds);

        List<AssessmentScoreRowInfo> result = new ArrayList<AssessmentScoreRowInfo>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            result.add(toScoreRow(row, classNames.get(candidateResultId), lastRounds.get(candidateResultId)));
        }
        return result;
    }

    private List<Tuple> findScoreSheetRows(GradingAssignmentFilter filter) {
        return applyFilters(em.createQuery("""
            SELECT cr.id, u.fullName, u.email, e.name, sch.startDate, cr.totalScore, rb.name,
                   cr.status, cr.releasedAt
            FROM AssessmentCandidateResultJpaEntity cr
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN AssessmentScheduleJpaEntity sch ON sch.id = c.scheduleId
            LEFT JOIN RubricResultBandJpaEntity rb ON rb.id = cr.rubricResultBandId
            """ + WHERE_CLAUSE + """
            ORDER BY u.fullName ASC
        """, Tuple.class), filter, Instant.now())
            .getResultList();
    }

    private AssessmentScoreRowInfo toScoreRow(Tuple row, String className, LastRound lastRound) {
        return new AssessmentScoreRowInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            row.get(2, String.class),
            className,
            row.get(3, String.class),
            row.get(4, Instant.class),
            row.get(5, BigDecimal.class),
            row.get(6, String.class),
            row.get(7, String.class),
            lastRound == null ? null : lastRound.roundType(),
            lastRound == null ? null : lastRound.outcome(),
            lastRound == null ? null : lastRound.teacherName(),
            row.get(8, Instant.class)
        );
    }

    private record LastRound(String roundType, String outcome, String teacherName) {
    }

    /** The most recently completed round of many attempts, 1 query; keeps the first row per attempt. */
    private Map<UUID, LastRound> lastCompletedRoundByResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ga.candidateResultId, ga.roundType, ga.outcome, t.fullName
            FROM AssessmentGradingAssignmentJpaEntity ga
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE ga.candidateResultId IN (:candidateResultIds)
            AND ga.status = :completed
            ORDER BY ga.completedAt DESC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .setParameter("completed", GradingAssignmentStatus.COMPLETED.name())
            .getResultList();
        Map<UUID, LastRound> map = new LinkedHashMap<UUID, LastRound>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), new LastRound(
                row.get(1, String.class), row.get(2, String.class), row.get(3, String.class)));
        }
        return map;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    }
}