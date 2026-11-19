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

import com.sep.vox.application.mapper.examgrading.GradingResultCode;
import com.sep.vox.application.query.dto.AssignableTeacherInfo;
import com.sep.vox.application.query.dto.BulkFinalizePreviewInfo;
import com.sep.vox.application.query.dto.ExamScoreRowInfo;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingCriterionMetaInfo;
import com.sep.vox.application.query.dto.GradingCriterionScoreInfo;
import com.sep.vox.application.query.dto.GradingExamOptionInfo;
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.infrastructure.persistence.entity.QuestionAssetJpaEntity;
import com.sep.vox.infrastructure.persistence.mapper.QuestionAssetMapper;
import com.sep.vox.application.query.dto.GradingTurnInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaExamGradingQueryRepository implements ExamGradingQueryRepository {

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
        + " SELECT 1 FROM ExamResultAppealJpaEntity ap"
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
    private static final int MAX_EXAM_OPTIONS = 200;

    /** So khớp với {@code ExamKind.CLASS_TEST}; JPQL trả enum về dạng String. */
    private static final String CLASS_TEST_KIND = "CLASS_TEST";

    /** So khớp với {@code ExamKind.CENTRALIZED}. Cùng lý do dùng String như trên. */
    private static final String CENTRALIZED_KIND = "CENTRALIZED";

    /** Trạng thái bài còn có thể nhận một vòng chấm — luật lấy từ domain, không chép cứng. */
    private static final List<String> ASSIGNABLE_STATUS_NAMES = GradingRoundPolicy.allAssignableStatuses()
        .stream()
        .map(status -> status.name())
        .toList();

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

        List<Tuple> rows = applyFilters(em.createQuery("""
            SELECT cr.id, u.fullName, e.name, cr.status, cr.totalScore, s.flagged, cr.updatedAt, cr.sessionId
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE + """
            ORDER BY cr.updatedAt DESC, cr.id ASC
        """, Tuple.class), filter, now)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size)
            .getResultList();

        List<UUID> candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, String> classNames = classNamesByCandidateResultIds(candidateResultIds);
        Map<UUID, DisplayAssignment> displayed = displayAssignmentsByResultIds(candidateResultIds);
        Set<UUID> openAppeals = resultIdsWithOpenAppeal(candidateResultIds);
        Map<UUID, AttemptInfo> attempts = attemptsByCandidateResultIds(candidateResultIds);

        List<GradingAssignmentRowInfo> content = new ArrayList<GradingAssignmentRowInfo>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            DisplayAssignment assignment = displayed.get(candidateResultId);
            AttemptInfo attempt = attemptOrSingle(attempts, candidateResultId, row.get(7, UUID.class));
            content.add(new GradingAssignmentRowInfo(
                candidateResultId,
                GradingResultCode.of(candidateResultId),
                row.get(1, String.class),
                classNames.get(candidateResultId),
                row.get(2, String.class),
                row.get(3, String.class),
                row.get(4, BigDecimal.class),
                Boolean.TRUE.equals(row.get(5, Boolean.class)),
                assignment == null ? null : assignment.id(),
                assignment == null ? null : assignment.teacherId(),
                assignment == null ? null : assignment.teacherName(),
                assignment == null ? null : assignment.roundType(),
                assignment == null ? null : assignment.status(),
                assignment == null ? null : assignment.outcome(),
                assignment == null ? null : assignment.assignedAt(),
                assignment == null ? null : assignment.completedAt(),
                assignment == null ? null : assignment.deadlineAt(),
                assignment != null && assignment.isOverdue(now),
                openAppeals.contains(candidateResultId),
                attempt.sessionId(),
                attempt.attemptNo(),
                attempt.attemptCount()
            ));
        }

        Long total = applyFilters(em.createQuery("""
            SELECT COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE, Long.class), filter, now)
            .getSingleResult();

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    /**
     * Mọi bộ lọc "theo phân công" đều là EXISTS chứ không phải JOIN — câu hỏi của
     * admin là "bài nào có một phân công thoả điều kiện", và EXISTS trả lời đúng câu
     * đó mà không nhân bản dòng.
     */
    private static final String WHERE_CLAUSE = """
        WHERE e.schoolId = :schoolId
        AND cr.status <> 'DELETED'
        AND (:examKind IS NULL OR e.kind = :examKind)
        AND (:examId IS NULL OR cr.examId = :examId)
        AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        AND (:resultStatus IS NULL OR cr.status = :resultStatus)
        AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword
             OR LOWER(CAST(cr.id AS string)) LIKE :keyword)
        AND (:teacherId IS NULL OR EXISTS (
            SELECT 1 FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.teacherId = :teacherId))
        AND (:roundType IS NULL OR EXISTS (
            SELECT 1 FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.roundType = :roundType))
        AND (:assignmentStatus IS NULL OR EXISTS (
            SELECT 1 FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.candidateResultId = cr.id AND ga.status = :assignmentStatus))
        AND (:unassignedOnly IS NULL OR NOT EXISTS (
            SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id))
        AND (:overdueOnly IS NULL OR EXISTS (
            SELECT 1 FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.activeResultId = cr.id AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now))
        AND (:hasOpenAppeal IS NULL OR :hasOpenAppeal = (
            CASE WHEN EXISTS (
                SELECT 1 FROM ExamResultAppealJpaEntity ap
                WHERE ap.candidateResultId = cr.id
                AND ap.status IN (""" + OPEN_APPEAL_STATUSES + """
            )) THEN TRUE ELSE FALSE END))
        """;

    private <T> jakarta.persistence.TypedQuery<T> applyFilters(
            jakarta.persistence.TypedQuery<T> query, GradingAssignmentFilter filter, Instant now) {
        return query
            .setParameter("schoolId", filter.schoolId())
            .setParameter("examKind", filter.examKind())
            .setParameter("examId", filter.examId())
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
        List<Tuple> rows = em.createQuery("""
            SELECT ga.candidateResultId, ga.id, ga.teacherId, t.fullName, ga.roundType, ga.status,
                   ga.outcome, ga.assignedAt, ga.completedAt, ga.deadlineAt, ga.activeResultId
            FROM ExamGradingAssignmentJpaEntity ga
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE ga.candidateResultId IN (:candidateResultIds)
            ORDER BY ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();

        Map<UUID, DisplayAssignment> map = new HashMap<UUID, DisplayAssignment>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            DisplayAssignment assignment = new DisplayAssignment(
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
            DisplayAssignment existing = map.get(candidateResultId);
            // An open row always wins; among closed rows the first one (newest) wins.
            if (existing == null || (assignment.open() && !existing.open())) {
                map.put(candidateResultId, assignment);
            }
        }
        return map;
    }

    private Set<UUID> resultIdsWithOpenAppeal(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT ap.candidateResultId FROM ExamResultAppealJpaEntity ap
            WHERE ap.candidateResultId IN (:candidateResultIds)
            AND ap.status IN (""" + OPEN_APPEAL_STATUSES + ")", UUID.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList());
    }

    /** Tên lớp của nhiều bài trong 1 query; giữ giá trị đầu tiên mỗi bài (tránh N+1). */
    /**
     * Student names in bulk. Only called for CLASS tests — centralized exams keep
     * anonymous grading, and that is a fairness guarantee, not a superfluous detail.
     */
    private Map<UUID, String> studentNamesByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, u.fullName
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
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
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
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
     * <p>There is no {@code attempt_no} column in the DB: an attempt is one {@code exam_sessions}
     * row, so the order must be derived from the {@code startedAt} of the same candidate.
     * Numbered ASCENDING to match how the student's "my exams" screen numbers them
     * ({@code ViewMyExamsUseCase}) — when both sides say "attempt 2" it must be the same attempt.
     *
     * <p>The condition {@code s.examId = cr.examId} is redundant as far as the data goes (every {@code exam_candidates}
     * row already belongs to exactly one exam) but it is kept as a guard rail:
     * counting an attempt of another exam would give a wrong number that nobody notices is wrong.
     */
    private Map<UUID, AttemptInfo> attemptsByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cr.id, cr.sessionId, s.id
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN ExamSessionJpaEntity s ON s.candidateId = c.id AND s.examId = cr.examId
            WHERE cr.id IN (:candidateResultIds)
            ORDER BY cr.id ASC, s.startedAt ASC, s.id ASC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();

        // Group by attempt first, then number: ORDER BY already takes care of the order, here only a running count is needed.
        Map<UUID, List<UUID>> sessionsByResult = new LinkedHashMap<UUID, List<UUID>>();
        Map<UUID, UUID> ownSessionByResult = new HashMap<UUID, UUID>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            ownSessionByResult.putIfAbsent(candidateResultId, row.get(1, UUID.class));
            sessionsByResult.computeIfAbsent(candidateResultId, ignored -> new ArrayList<>())
                .add(row.get(2, UUID.class));
        }

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
    public GradingStatsInfo stats(UUID schoolId, UUID examId, UUID scheduleId, String examKind) {
        Instant now = Instant.now();

        List<Tuple> statusRows = em.createQuery("""
            SELECT cr.status, COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND cr.status <> 'DELETED'
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY cr.status
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        List<GradingStatsInfo.ResultStatusCount> byResultStatus = new ArrayList<GradingStatsInfo.ResultStatusCount>();
        int total = 0;
        for (Tuple row : statusRows) {
            int count = row.get(1, Long.class).intValue();
            byResultStatus.add(new GradingStatsInfo.ResultStatusCount(row.get(0, String.class), count));
            total += count;
        }

        // One query for all three assignment numbers: open, overdue, and (derived) unassigned.
        Tuple openRow = em.createQuery("""
            SELECT COUNT(ga),
                   SUM(CASE WHEN ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now THEN 1 ELSE 0 END)
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.activeResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("now", now)
            .getSingleResult();

        int assigned = openRow.get(0, Long.class).intValue();
        Long overdueSum = openRow.get(1, Long.class);
        int overdue = overdueSum == null ? 0 : overdueSum.intValue();

        // "Unassigned" must count exactly what it promises: attempts that are ELIGIBLE for a grading
        // round but have no open assignment. The old way (total - assigned) also included attempts whose
        // lifecycle had ended, so the number kept growing up to the total number of attempts once the exam
        // was fully graded -- exactly when it should drop to 0.
        Long unassigned = em.createQuery("""
            SELECT COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND cr.status IN (:assignableStatuses)
            AND NOT EXISTS (
                SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
            )
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("assignableStatuses", ASSIGNABLE_STATUS_NAMES)
            .getSingleResult();

        List<Tuple> progressRows = em.createQuery("""
            SELECT ga.teacherId, t.fullName,
                   SUM(CASE WHEN ga.status = 'ASSIGNED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ga.status = 'COMPLETED' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL
                            AND ga.deadlineAt < :now THEN 1 ELSE 0 END)
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY ga.teacherId, t.fullName
            ORDER BY t.fullName ASC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("now", now)
            .getResultList();

        List<GradingStatsInfo.TeacherProgress> teacherProgress = new ArrayList<GradingStatsInfo.TeacherProgress>();
        for (Tuple row : progressRows) {
            teacherProgress.add(new GradingStatsInfo.TeacherProgress(
                row.get(0, UUID.class),
                row.get(1, String.class),
                intOf(row.get(2, Long.class)),
                intOf(row.get(3, Long.class)),
                intOf(row.get(4, Long.class))
            ));
        }

        return new GradingStatsInfo(
            total, byResultStatus, unassigned.intValue(), assigned, overdue, teacherProgress);
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
            UUID teacherId, String examKind, String status, String roundType, int page, int size) {
        return findTasksByTeacherIdAndExamId(teacherId, null, examKind, status, roundType, page, size);
    }

    @Override
    public PageResult<GradingTaskInfo> findTasksByTeacherIdAndExamId(
            UUID teacherId, UUID examId, String examKind, String status, String roundType,
            int page, int size) {
        Instant now = Instant.now();

        // Anonymity is a rule of CENTRALIZED exams, not a general rule: there the student data
        // must not leave this read model. A class test is the opposite --
        // the grader is the very teacher who teaches that class. So only e.kind is fetched here and then
        // identities are loaded in bulk for EXACTLY the CLASS_TEST rows (see below).
        List<Tuple> rows = em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, cr.totalScore,
                   s.flagged, ga.assignedAt, ga.deadlineAt, e.kind, cr.sessionId
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE ga.teacherId = :teacherId
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
            ORDER BY ga.status ASC, ga.deadlineAt ASC NULLS LAST, ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("teacherId", teacherId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("status", status)
            .setParameter("roundType", roundType)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size)
            .getResultList();

        List<UUID> pageResultIds = rows.stream().map(row -> row.get(1, UUID.class)).toList();
        Map<UUID, Integer> partCounts = partCountsByCandidateResultIds(pageResultIds);
        Map<UUID, AttemptInfo> attempts = attemptsByCandidateResultIds(pageResultIds);

        // Two extra queries, run ONLY when the page contains a class-test row -- the number of
        // queries stays fixed per page, no N+1.
        List<UUID> classTestResultIds = rows.stream()
            .filter(row -> CLASS_TEST_KIND.equals(row.get(10, String.class)))
            .map(row -> row.get(1, UUID.class))
            .toList();
        Map<UUID, String> studentNames = studentNamesByCandidateResultIds(classTestResultIds);
        Map<UUID, String> classNames = classNamesByCandidateResultIds(classTestResultIds);

        List<GradingTaskInfo> content = new ArrayList<GradingTaskInfo>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(1, UUID.class);
            String assignmentStatus = row.get(4, String.class);
            Instant deadlineAt = row.get(9, Instant.class);
            AttemptInfo attempt = attemptOrSingle(attempts, candidateResultId, row.get(11, UUID.class));
            content.add(new GradingTaskInfo(
                row.get(0, UUID.class),
                candidateResultId,
                GradingResultCode.of(candidateResultId),
                row.get(2, String.class),
                partCounts.getOrDefault(candidateResultId, 0),
                row.get(3, String.class),
                assignmentStatus,
                row.get(5, String.class),
                row.get(6, BigDecimal.class),
                Boolean.TRUE.equals(row.get(7, Boolean.class)),
                row.get(8, Instant.class),
                deadlineAt,
                isOverdue(assignmentStatus, deadlineAt, now),
                studentNames.get(candidateResultId),
                classNames.get(candidateResultId),
                attempt.sessionId(),
                attempt.attemptNo(),
                attempt.attemptCount()
            ));
        }

        // The JOIN to exams here is REQUIRED even though this statement reads no exam column: without
        // it COUNT counts both kinds of attempt while the page returns only one kind, and the FE builds
        // empty pages that cannot be clicked.
        Long total = em.createQuery("""
            SELECT COUNT(ga) FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE ga.teacherId = :teacherId
            AND (:examKind IS NULL OR e.kind = :examKind)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
        """, Long.class)
            .setParameter("teacherId", teacherId)
            .setParameter("examKind", examKind)
            .setParameter("examId", examId)
            .setParameter("status", status)
            .setParameter("roundType", roundType)
            .getSingleResult();

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    /**
     * Derive the exam scope FROM the set of assignments, not from the exams table: a grading teacher is not
     * a member of a centralized exam, so every normal read path over exams is closed to
     * them and asking there only returns an empty list.
     *
     * <p>COMPLETED assignments are counted too — the filter must still be able to select an exam that was just fully graded,
     * otherwise submitting the last attempt makes the whole exam vanish from the dropdown right under their hand.
     *
     * <p>Join through {@code ga.candidateResultId}, NOT {@code ga.activeResultId}:
     * the latter is null on every closed row, and using it would lose the whole "has graded before" half.
     */
    @Override
    public List<GradingExamOptionInfo> findExamsWithTasksByTeacherId(UUID teacherId, String examKind) {
        // GROUP BY instead of SELECT DISTINCT: it yields the same distinct set of exams, but also gets
        // the two counts in one pass. With DISTINCT every row would need its own count query
        // -- a genuine N+1 for a single dropdown.
        List<Tuple> rows = em.createQuery("""
            SELECT e.id, e.code, e.name, COUNT(ga),
                   SUM(CASE WHEN ga.status = :assignedStatus THEN 1 ELSE 0 END)
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE ga.teacherId = :teacherId
            AND (:examKind IS NULL OR e.kind = :examKind)
            GROUP BY e.id, e.code, e.name
            ORDER BY MAX(ga.assignedAt) DESC, e.name ASC
        """, Tuple.class)
            .setParameter("teacherId", teacherId)
            .setParameter("examKind", examKind)
            .setParameter("assignedStatus", GradingAssignmentStatus.ASSIGNED.name())
            .setMaxResults(MAX_EXAM_OPTIONS)
            .getResultList();

        List<GradingExamOptionInfo> options = new ArrayList<GradingExamOptionInfo>();
        for (Tuple row : rows) {
            options.add(new GradingExamOptionInfo(
                row.get(0, UUID.class),
                row.get(1, String.class),
                row.get(2, String.class),
                longOf(row.get(3, Long.class)),
                longOf(row.get(4, Long.class))
            ));
        }
        return options;
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
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamItemResponseJpaEntity r ON r.sessionId = cr.sessionId
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
        // This query runs ALONE and before every other query -- it is the authorization
        // gate. No exam-slot query may run before it has returned a row.
        List<Tuple> rows = em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, s.flagged, s.flagReason,
                   cr.totalScore, ga.scoreBefore, ga.deadlineAt, cr.rubricVersionId, cr.sessionId, ap.reason,
                   e.kind
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            LEFT JOIN ExamResultAppealJpaEntity ap ON ap.id = ga.appealId
            WHERE ga.id = :assignmentId AND ga.teacherId = :teacherId
        """, Tuple.class)
            .setParameter("assignmentId", assignmentId)
            .setParameter("teacherId", teacherId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Tuple row = rows.get(0);
        UUID candidateResultId = row.get(1, UUID.class);
        String roundType = row.get(3, String.class);
        String assignmentStatus = row.get(4, String.class);
        String resultStatus = row.get(5, String.class);
        Instant deadlineAt = row.get(10, Instant.class);
        boolean isClassTest = CLASS_TEST_KIND.equals(row.get(14, String.class));
        UUID sessionId = row.get(12, UUID.class);
        AttemptInfo attempt = attemptOrSingle(
            attemptsByCandidateResultIds(List.of(candidateResultId)), candidateResultId, sessionId);
        Instant now = Instant.now();

        // The read-only flag and the button list are decided by the BE, the FE does not derive them: the round x action
        // rules live in GradingRoundPolicy, and re-deriving them in the FE means the two sides will drift apart sooner or later.
        GradingRoundType round = GradingRoundType.valueOf(roundType);
        boolean editable = !GradingAssignmentStatus.COMPLETED.name().equals(assignmentStatus)
            && GradingRoundPolicy.isAssignable(round, ExamCandidateResultStatus.valueOf(resultStatus));
        List<String> allowedOutcomes = editable
            ? GradingRoundPolicy.allowedOutcomes(round).stream().map(outcome -> outcome.name()).sorted().toList()
            : List.<String>of();

        return Optional.of(new GradingTaskDetailInfo(
            row.get(0, UUID.class),
            candidateResultId,
            GradingResultCode.of(candidateResultId),
            row.get(2, String.class),
            roundType,
            assignmentStatus,
            resultStatus,
            Boolean.TRUE.equals(row.get(6, Boolean.class)),
            row.get(7, String.class),
            row.get(8, BigDecimal.class),
            row.get(9, BigDecimal.class),
            deadlineAt,
            isOverdue(assignmentStatus, deadlineAt, now),
            editable,
            allowedOutcomes,
            row.get(13, String.class),
            taskItems(sessionId),
            criteria(row.get(11, UUID.class)),
            // A centralized exam keeps its anonymous grading -- the two lookups below do not run.
            isClassTest ? studentNamesByCandidateResultIds(List.of(candidateResultId)).get(candidateResultId) : null,
            isClassTest ? classNamesByCandidateResultIds(List.of(candidateResultId)).get(candidateResultId) : null,
            attempt.sessionId(),
            attempt.attemptNo(),
            attempt.attemptCount()
        ));
    }

    /**
     * The exam parts of the attempt with the data needed for grading. The number of queries is fixed (4) no matter how
     * many parts the attempt has.
     *
     * <p>The two different evaluation lookups are intentional: the reference score/comment comes from the
     * evaluation currently in effect, while the spoken turns must come from the AI version because only the AI version
     * produces turns.
     */
    private List<GradingTaskItemInfo> taskItems(UUID sessionId) {
        List<Tuple> rows = em.createQuery("""
            SELECT r.id, r.paperItemId, sec.title, pi.sectionId, q.questionText, pi.questionId
            FROM ExamItemResponseJpaEntity r
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = r.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            LEFT JOIN QuestionJpaEntity q ON q.id = pi.questionId
            WHERE r.sessionId = :sessionId
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("sessionId", sessionId)
            .getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> responseIds = rows.stream().map(row -> row.get(0, UUID.class)).filter(Objects::nonNull).toList();
        Map<UUID, CurrentEvaluation> currentEvaluations = currentEvaluationsByResponseIds(responseIds);
        Map<UUID, AiEvaluation> aiEvaluations = aiEvaluationsByResponseIds(responseIds);
        // One query for BOTH versions: two separate calls would only cost an extra round-trip and
        // would still have to group by evaluationId in exactly the same way.
        List<UUID> scoreEvaluationIds = new ArrayList<UUID>();
        currentEvaluations.values().forEach(evaluation -> scoreEvaluationIds.add(evaluation.id()));
        aiEvaluations.values().forEach(evaluation -> scoreEvaluationIds.add(evaluation.id()));
        Map<UUID, List<GradingCriterionScoreInfo>> scoresByEvaluation = criterionScoresByEvaluationIds(scoreEvaluationIds);
        Map<UUID, List<GradingTurnInfo>> turnsByEvaluation = turnsByEvaluationIds(
            aiEvaluations.values().stream().map(evaluation -> evaluation.id()).toList());
        // A question that has NEVER been graded by the AI has no row in exam_item_evaluation_turns,
        // so the grading screen shows up COMPLETELY EMPTY -- no transcript, no audio, not even the question.
        //
        // Measured on production 2026-08-16, session 01a00e64: the attempt was force-ended during the
        // exam so the AI had not graded any question yet. Result: 0 evaluation, 0 evaluation_turn --
        // BUT 2 responses and 3 response_turns were intact, with both the transcript and the audio_url pointing to
        // S3. The evidence was still there, the grading screen just did not look where it lives. The teacher opened it
        // and saw a blank page with no way to grade by hand.
        //
        // Only queried when a question is REALLY missing: a normally graded attempt keeps exactly the 4 queries
        // stated in the comment at the top of the method, with no extra round-trip.
        List<UUID> responseIdsWithoutAiTurns = responseIds.stream()
            .filter(responseId -> {
                AiEvaluation ai = aiEvaluations.get(responseId);
                return ai == null || turnsByEvaluation.getOrDefault(ai.id(), List.of()).isEmpty();
            })
            .toList();
        Map<UUID, List<GradingTurnInfo>> fallbackTurnsByResponse = turnsByResponseIds(responseIdsWithoutAiTurns);
        Map<UUID, QuestionAssetDto> assetsByQuestion = questionAssetsByQuestionIds(
            rows.stream().map(row -> row.get(5, UUID.class)).filter(Objects::nonNull).distinct().toList());

        // Question number within the section: rows are already sorted by (sec.order, pi.order), so a running
        // count per sectionId is enough. A question that belongs to no section becomes question 1.
        Map<UUID, Integer> seenPerSection = new HashMap<UUID, Integer>();
        List<GradingTaskItemInfo> result = new ArrayList<GradingTaskItemInfo>();
        for (Tuple row : rows) {
            UUID responseId = row.get(0, UUID.class);
            UUID sectionId = row.get(3, UUID.class);
            CurrentEvaluation current = currentEvaluations.get(responseId);
            AiEvaluation ai = aiEvaluations.get(responseId);
            int orderInSection;
            if (sectionId == null) {
                orderInSection = 1;
            } else {
                Integer seen = seenPerSection.get(sectionId);
                orderInSection = seen == null ? 1 : seen + 1;
                seenPerSection.put(sectionId, orderInSection);
            }
            result.add(new GradingTaskItemInfo(
                row.get(1, UUID.class),
                responseId,
                row.get(2, String.class),
                sectionId,
                orderInSection,
                // LEFT JOIN to the question inside the existing query: no extra round-trip,
                // keeping the "fixed number of queries" stated at the top of the method.
                row.get(4, String.class),
                assetsByQuestion.get(row.get(5, UUID.class)),
                current == null ? null : current.itemScore(),
                current == null ? null : current.feedbackSummary(),
                current == null ? List.of() : scoresByEvaluation.getOrDefault(current.id(), List.of()),
                turnsForItem(responseId, ai, turnsByEvaluation, fallbackTurnsByResponse),
                ai == null ? List.of() : scoresByEvaluation.getOrDefault(ai.id(), List.of()),
                ai == null ? null : ai.overallConfidence(),
                ai == null ? null : ai.feedbackSummary(),
                ai != null && ai.requiresHumanReview(),
                ai == null ? null : ai.reviewReasonCode(),
                ai != null && ai.markedInvalid(),
                ai != null && ai.requiresRetake(),
                ai == null ? null : ai.signals(),
                ai == null ? null : ai.validityJson()
            ));
        }
        return result;
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
     * {@code GetExamSessionPaperUseCase} ({@code findByQuestionId(...).findFirst()}) — so the grading screen
     * and the exam screen never look at two different assets of the same question.
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
            FROM ExamItemEvaluationJpaEntity ev
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
        List<Tuple> rows = em.createQuery("""
            SELECT ev.responseId, ev.id, ev.overallConfidence, ev.feedbackSummary,
                   ev.requiresHumanReview, ev.reviewReasonCode, ev.markedInvalid,
                   ev.requiresRetake, ev.signals, ev.validityJson
            FROM ExamItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, AiEvaluation> map = new LinkedHashMap<UUID, AiEvaluation>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), new AiEvaluation(
                row.get(1, UUID.class),
                row.get(2, BigDecimal.class),
                row.get(3, String.class),
                Boolean.TRUE.equals(row.get(4, Boolean.class)),
                row.get(5, String.class),
                Boolean.TRUE.equals(row.get(6, Boolean.class)),
                Boolean.TRUE.equals(row.get(7, Boolean.class)),
                row.get(8, String.class),
                row.get(9, String.class)
            ));
        }
        return map;
    }

    /** Criterion scores of many evaluations in 1 query, grouped by evaluationId. */
    private Map<UUID, List<GradingCriterionScoreInfo>> criterionScoresByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT cs.evaluationId, cs.rubricCriterionId, rc.code, rc.name, cs.finalScore, cs.rationale
            FROM ExamItemCriterionScoreJpaEntity cs
            JOIN RubricCriterionJpaEntity rc ON rc.id = cs.rubricCriterionId
            WHERE cs.evaluationId IN (:evaluationIds)
            ORDER BY rc.order ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        Map<UUID, List<GradingCriterionScoreInfo>> map = new LinkedHashMap<UUID, List<GradingCriterionScoreInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new GradingCriterionScoreInfo(
                    row.get(1, UUID.class),
                    row.get(2, String.class),
                    row.get(3, String.class),
                    row.get(4, BigDecimal.class),
                    row.get(5, String.class)
                ));
        }
        return map;
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
     * The candidate's ORIGINAL spoken turns, read straight from {@code exam_item_response_turns}.
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
            SELECT t.examItemResponseId, t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl,
                   t.transcript, t.durationSeconds, t.wordCount
            FROM ExamItemResponseTurnJpaEntity t
            WHERE t.examItemResponseId IN (:responseIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, List<GradingTurnInfo>> map = new LinkedHashMap<UUID, List<GradingTurnInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new GradingTurnInfo(
                    row.get(1, UUID.class),
                    row.get(2, Integer.class),
                    row.get(3, String.class),
                    row.get(4, String.class),
                    row.get(5, String.class),
                    row.get(6, String.class),
                    row.get(7, Integer.class),
                    row.get(8, Integer.class),
                    null,
                    null,
                    null
                ));
        }
        return map;
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
            FROM ExamItemEvaluationTurnJpaEntity t
            WHERE t.evaluationId IN (:evaluationIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        Map<UUID, List<GradingTurnInfo>> map = new LinkedHashMap<UUID, List<GradingTurnInfo>>();
        for (Tuple row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new GradingTurnInfo(
                    row.get(1, UUID.class),
                    row.get(2, Integer.class),
                    row.get(3, String.class),
                    row.get(4, String.class),
                    row.get(5, String.class),
                    row.get(6, String.class),
                    row.get(7, Integer.class),
                    row.get(8, Integer.class),
                    row.get(9, Double.class),
                    row.get(10, String.class),
                    row.get(11, String.class)
                ));
        }
        return map;
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
        String normalizedKeyword = normalizeKeyword(keyword);
        List<Tuple> rows = em.createQuery("""
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
            FROM ExamGradingAssignmentJpaEntity ga
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

    @Override
    public List<UUID> findAssignableResultIds(
            UUID schoolId, UUID examId, UUID scheduleId, Collection<String> resultStatuses) {
        if (resultStatuses == null || resultStatuses.isEmpty()) {
            return List.of();
        }
        // NOT EXISTS trên active_result_id thay vì LEFT JOIN ... IS NULL: unique index
        // nằm đúng ở cột đó, và cách này giữ auto-assign chạy lại nhiều lần vẫn an toàn.
        //
        // Bài đang có đơn phúc khảo mở cũng bị loại — vòng APPEAL do luồng phúc khảo
        // giao, không đi qua auto-assign, và giao vòng khác lúc đó sẽ có hai người
        // cùng ghi điểm một bài (review BE-4).
        //
        // Loại bài kiểm tra trên lớp khoá cứng ở đây chứ không nhận tham số: auto-assign
        // là hành động của nhà trường, mà bài trên lớp thì nhà trường không phân công.
        return em.createQuery("""
            SELECT cr.id
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND e.kind = :centralizedKind
            AND cr.status IN (:resultStatuses)
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND NOT EXISTS (
                SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
            )
            AND NOT EXISTS (
                SELECT 1 FROM ExamResultAppealJpaEntity ap
                WHERE ap.candidateResultId = cr.id AND ap.status IN (""" + OPEN_APPEAL_STATUSES + """
                )
            )
            ORDER BY cr.id ASC
        """, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("centralizedKind", CENTRALIZED_KIND)
            .setParameter("resultStatuses", resultStatuses)
            .setParameter("examId", examId)
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
            FROM ExamCandidateResultJpaEntity cr
            LEFT JOIN AssessmentPolicyJpaEntity pol ON pol.id = cr.assessmentPolicyId
            LEFT JOIN ExamItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            LEFT JOIN ExamItemEvaluationJpaEntity ev ON ev.responseId = r.id
                AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
            WHERE cr.id IN (:candidateResultIds)
            GROUP BY cr.id, cr.totalScore, pol.passingScore
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();

        List<GradingRiskInfo> result = new ArrayList<GradingRiskInfo>();
        for (Tuple row : rows) {
            result.add(new GradingRiskInfo(
                row.get(0, UUID.class),
                row.get(1, BigDecimal.class),
                row.get(2, BigDecimal.class),
                row.get(3, BigDecimal.class)
            ));
        }
        return result;
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
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            JOIN ExamItemEvaluationJpaEntity ev ON ev.responseId = r.id
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
     * row of the exam into Java and counted with a loop — acceptable at the current scale,
     * but this is a pure aggregate so there is no reason for the data to leave the DB.
     */
    @Override
    public BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID examId) {
        Tuple counts = em.createQuery("""
            SELECT COUNT(cr),
                   SUM(CASE WHEN cr.status = :pendingReview AND NOT EXISTS (
                       SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
                   ) THEN 1 ELSE 0 END),
                   SUM(CASE WHEN cr.status = :pendingReview AND EXISTS (
                       SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
                   ) THEN 1 ELSE 0 END),
                   SUM(CASE WHEN """ + OPEN_APPEAL_EXISTS + """
                        THEN 1 ELSE 0 END),
                   SUM(CASE WHEN cr.status = :invalid THEN 1 ELSE 0 END),
                   SUM(CASE WHEN """ + BLOCKING_CONDITION + """
                        THEN 1 ELSE 0 END)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE e.schoolId = :schoolId AND cr.examId = :examId AND cr.status <> 'DELETED'
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("pendingReview", ExamCandidateResultStatus.PENDING_REVIEW.name())
            .setParameter("invalid", ExamCandidateResultStatus.INVALID.name())
            .getSingleResult();

        int total = intOf(counts.get(0, Long.class));
        int blocked = intOf(counts.get(5, Long.class));

        // Sample list: the UI only needs a few examples so the admin recognizes the problem, the total count is
        // already in the count fields above. Not fetched when nothing is blocking.
        List<UUID> blockingResultIds = blocked == 0 ? List.<UUID>of() : em.createQuery("""
            SELECT cr.id
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE e.schoolId = :schoolId AND cr.examId = :examId
            AND """ + BLOCKING_CONDITION + """
            ORDER BY cr.id ASC
        """, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("pendingReview", ExamCandidateResultStatus.PENDING_REVIEW.name())
            .setMaxResults(BLOCKING_SAMPLE_SIZE)
            .getResultList();

        return new BulkFinalizePreviewInfo(
            total,
            total - blocked,
            intOf(counts.get(1, Long.class)),
            intOf(counts.get(2, Long.class)),
            intOf(counts.get(3, Long.class)),
            intOf(counts.get(4, Long.class)),
            List.copyOf(blockingResultIds));
    }

    /**
     * The score sheet for file export. Two steps: fetch the main rows, then load the most recent grading round in
     * bulk — no query repeated per student.
     *
     * <p>The "exam shift" column is the schedule's {@code startDate}, not a name:
     * {@code exam_schedules} has no name column, a shift is identified by its start time
     * (the previous version selected {@code sch.name}, so this statement threw {@code UnknownPathException}
     * on every export).
     *
     * <p>Reuses {@link #WHERE_CLAUSE} and {@link #applyFilters} of the dispatch table instead of
     * writing a separate WHERE clause. The previous version wrote its own and FORGOT the {@code e.kind} condition
     * — every other statement in this class has it — so an export from the class-test screen still pulled in
     * the scores of centralized exams of the same school. With one shared predicate the exported file
     * cannot drift from the table the user is looking at.
     *
     * <p>The aliases that {@code WHERE_CLAUSE} needs ({@code cr}, {@code e}, {@code c},
     * {@code u}) are all already in the FROM below; it does not touch {@code s}
     * ({@code ExamSessionJpaEntity}) so no extra join is needed.
     */
    @Override
    public List<ExamScoreRowInfo> findScoreRows(GradingAssignmentFilter filter) {
        List<Tuple> rows = applyFilters(em.createQuery("""
            SELECT cr.id, u.fullName, u.email, e.name, sch.startDate, cr.totalScore, rb.name,
                   cr.status, cr.releasedAt
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamScheduleJpaEntity sch ON sch.id = c.scheduleId
            LEFT JOIN RubricResultBandJpaEntity rb ON rb.id = cr.rubricResultBandId
            """ + WHERE_CLAUSE + """
            ORDER BY u.fullName ASC
        """, Tuple.class), filter, Instant.now())
            .getResultList();

        List<UUID> candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, String> classNames = classNamesByCandidateResultIds(candidateResultIds);
        Map<UUID, LastRound> lastRounds = lastCompletedRoundByResultIds(candidateResultIds);

        List<ExamScoreRowInfo> result = new ArrayList<ExamScoreRowInfo>();
        for (Tuple row : rows) {
            UUID candidateResultId = row.get(0, UUID.class);
            LastRound lastRound = lastRounds.get(candidateResultId);
            result.add(new ExamScoreRowInfo(
                candidateResultId,
                row.get(1, String.class),
                row.get(2, String.class),
                classNames.get(candidateResultId),
                row.get(3, String.class),
                row.get(4, Instant.class),
                row.get(5, BigDecimal.class),
                row.get(6, String.class),
                row.get(7, String.class),
                lastRound == null ? null : lastRound.roundType(),
                lastRound == null ? null : lastRound.outcome(),
                lastRound == null ? null : lastRound.teacherName(),
                row.get(8, Instant.class)
            ));
        }
        return result;
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
            FROM ExamGradingAssignmentJpaEntity ga
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