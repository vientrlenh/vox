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
import com.sep.vox.application.query.dto.GradingRiskInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
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
     * Trần số dòng một trang. {@code size} trong schema chỉ là giá trị MẶC ĐỊNH, không
     * phải giới hạn — không kẹp ở đây thì một client gửi {@code size: 100000} là đủ
     * làm nghẽn connection pool, kèm theo các query phụ với mệnh đề IN khổng lồ.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /** Trạng thái bài còn có thể nhận một vòng chấm — luật lấy từ domain, không chép cứng. */
    private static final List<String> ASSIGNABLE_STATUS_NAMES = GradingRoundPolicy.allAssignableStatuses()
        .stream()
        .map(status -> status.name())
        .toList();

    @PersistenceContext
    private EntityManager em;

    // ---- bảng điều phối (school admin) --------------------------------------

    /**
     * Ba bước, số query cố định (5) bất kể trang có bao nhiêu dòng: (1) phân trang
     * chính trên candidate_result, (2) nạp phân công của đúng trang đó, (3) nạp lớp +
     * cờ phúc khảo.
     *
     * <p>Không LEFT JOIN thẳng sang assignment như bản cũ: một bài giờ có NHIỀU dòng
     * phân công qua các vòng, join thẳng sẽ nhân bản dòng và làm hỏng cả phân trang
     * lẫn COUNT.
     */
    @Override
    public PageResult<GradingAssignmentRowInfo> searchAssignments(
            GradingAssignmentFilter filter, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var now = Instant.now();

        var rows = applyFilters(em.createQuery("""
            SELECT cr.id, u.fullName, e.name, cr.status, cr.totalScore, s.flagged, cr.updatedAt
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE + """
            ORDER BY cr.updatedAt DESC, cr.id ASC
        """, Tuple.class), filter, now)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var classNames = classNamesByCandidateResultIds(candidateResultIds);
        var displayed = displayAssignmentsByResultIds(candidateResultIds);
        var openAppeals = resultIdsWithOpenAppeal(candidateResultIds);

        var content = new ArrayList<GradingAssignmentRowInfo>();
        for (var row : rows) {
            var candidateResultId = row.get(0, UUID.class);
            var assignment = displayed.get(candidateResultId);
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
                openAppeals.contains(candidateResultId)
            ));
        }

        var total = applyFilters(em.createQuery("""
            SELECT COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            """ + WHERE_CLAUSE, Long.class), filter, now)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    /**
     * Mọi bộ lọc "theo phân công" đều là EXISTS chứ không phải JOIN — câu hỏi của
     * admin là "bài nào có một phân công thoả điều kiện", và EXISTS trả lời đúng câu
     * đó mà không nhân bản dòng.
     */
    private static final String WHERE_CLAUSE = """
        WHERE e.schoolId = :schoolId
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
     * Phân công để hiển thị cho mỗi bài: dòng đang mở nếu có, không thì dòng gần nhất
     * đã đóng — admin vẫn thấy được ai vừa chấm bài này ở vòng trước.
     */
    private Map<UUID, DisplayAssignment> displayAssignmentsByResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ga.candidateResultId, ga.id, ga.teacherId, t.fullName, ga.roundType, ga.status,
                   ga.outcome, ga.assignedAt, ga.completedAt, ga.deadlineAt, ga.activeResultId
            FROM ExamGradingAssignmentJpaEntity ga
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE ga.candidateResultId IN (:candidateResultIds)
            ORDER BY ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();

        var map = new HashMap<UUID, DisplayAssignment>();
        for (var row : rows) {
            var candidateResultId = row.get(0, UUID.class);
            var assignment = new DisplayAssignment(
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
            var existing = map.get(candidateResultId);
            // Dòng mở luôn thắng; giữa các dòng đã đóng thì dòng đầu (mới nhất) thắng.
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
    private Map<UUID, String> classNamesByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT cr.id, sc.name
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN SchoolClassUserJpaEntity scu ON scu.userId = c.studentId AND scu.isActive = true
            JOIN SchoolClassJpaEntity sc ON sc.id = scu.schoolClassId
            WHERE cr.id IN (:candidateResultIds)
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
        var map = new HashMap<UUID, String>();
        for (var row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, String.class));
        }
        return map;
    }

    // ---- thẻ số đầu màn ----------------------------------------------------

    @Override
    public GradingStatsInfo stats(UUID schoolId, UUID examId, UUID scheduleId) {
        var now = Instant.now();

        var statusRows = em.createQuery("""
            SELECT cr.status, COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY cr.status
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        var byResultStatus = new ArrayList<GradingStatsInfo.ResultStatusCount>();
        var total = 0;
        for (var row : statusRows) {
            var count = row.get(1, Long.class).intValue();
            byResultStatus.add(new GradingStatsInfo.ResultStatusCount(row.get(0, String.class), count));
            total += count;
        }

        // Một query cho cả ba con số phân công: đang mở, quá hạn, và (suy ra) chưa gán.
        var openRow = em.createQuery("""
            SELECT COUNT(ga),
                   SUM(CASE WHEN ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now THEN 1 ELSE 0 END)
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.activeResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("now", now)
            .getSingleResult();

        var assigned = openRow.get(0, Long.class).intValue();
        var overdueSum = openRow.get(1, Long.class);
        var overdue = overdueSum == null ? 0 : overdueSum.intValue();

        // "Chưa phân công" phải đếm đúng thứ nó hứa: bài ĐỦ ĐIỀU KIỆN nhận một vòng
        // chấm mà chưa có phân công mở. Cách cũ (total - assigned) gộp cả bài đã kết
        // thúc vòng đời vào, nên con số phình dần tới bằng tổng số bài khi kỳ thi đã
        // chấm xong — đúng lúc nó phải về 0.
        var unassigned = em.createQuery("""
            SELECT COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND cr.status IN (:assignableStatuses)
            AND NOT EXISTS (
                SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.activeResultId = cr.id
            )
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("assignableStatuses", ASSIGNABLE_STATUS_NAMES)
            .getSingleResult();

        var progressRows = em.createQuery("""
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
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY ga.teacherId, t.fullName
            ORDER BY t.fullName ASC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("now", now)
            .getResultList();

        var teacherProgress = new ArrayList<GradingStatsInfo.TeacherProgress>();
        for (var row : progressRows) {
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
            UUID teacherId, String status, String roundType, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var now = Instant.now();

        // Không join sang candidate/user: giáo viên chấm ẩn danh, dữ liệu học sinh
        // không được rời khỏi read model này.
        var rows = em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, cr.totalScore,
                   s.flagged, ga.assignedAt, ga.deadlineAt
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE ga.teacherId = :teacherId
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
            ORDER BY ga.status ASC, ga.deadlineAt ASC NULLS LAST, ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .setParameter("roundType", roundType)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var partCounts = partCountsByCandidateResultIds(
            rows.stream().map(row -> row.get(1, UUID.class)).toList());

        var content = new ArrayList<GradingTaskInfo>();
        for (var row : rows) {
            var candidateResultId = row.get(1, UUID.class);
            var assignmentStatus = row.get(4, String.class);
            var deadlineAt = row.get(9, Instant.class);
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
                isOverdue(assignmentStatus, deadlineAt, now)
            ));
        }

        var total = em.createQuery("""
            SELECT COUNT(ga) FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.teacherId = :teacherId
            AND (:status IS NULL OR ga.status = :status)
            AND (:roundType IS NULL OR ga.roundType = :roundType)
        """, Long.class)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .setParameter("roundType", roundType)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    private boolean isOverdue(String assignmentStatus, Instant deadlineAt, Instant now) {
        return GradingAssignmentStatus.ASSIGNED.name().equals(assignmentStatus)
            && deadlineAt != null && deadlineAt.isBefore(now);
    }

    /** Số phần phải chấm của nhiều bài trong 1 query — không đếm lẻ theo từng dòng. */
    private Map<UUID, Integer> partCountsByCandidateResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT cr.id, COUNT(r)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            WHERE cr.id IN (:candidateResultIds)
            GROUP BY cr.id
        """, Tuple.class)
            .setParameter("candidateResultIds", candidateResultIds)
            .getResultList();
        var map = new HashMap<UUID, Integer>();
        for (var row : rows) {
            map.put(row.get(0, UUID.class), row.get(1, Long.class).intValue());
        }
        return map;
    }

    // ---- màn chấm ----------------------------------------------------------

    @Override
    public Optional<GradingTaskDetailInfo> findTaskDetail(UUID assignmentId, UUID teacherId) {
        // Query này chạy MỘT MÌNH và trước mọi query khác — nó chính là chốt phân
        // quyền. Không được để query phần thi nào chạy trước khi nó trả về dòng.
        var rows = em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.roundType, ga.status, cr.status, s.flagged, s.flagReason,
                   cr.totalScore, ga.scoreBefore, ga.deadlineAt, cr.rubricVersionId, cr.sessionId, ap.reason
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
        var row = rows.get(0);
        var candidateResultId = row.get(1, UUID.class);
        var roundType = row.get(3, String.class);
        var assignmentStatus = row.get(4, String.class);
        var resultStatus = row.get(5, String.class);
        var deadlineAt = row.get(10, Instant.class);
        var now = Instant.now();

        // Cờ chỉ-đọc và danh sách nút do BE quyết, FE không tự suy: luật vòng × hành
        // động nằm ở GradingRoundPolicy, suy lại ở FE là sớm muộn hai bên lệch nhau.
        var round = GradingRoundType.valueOf(roundType);
        var editable = !GradingAssignmentStatus.COMPLETED.name().equals(assignmentStatus)
            && GradingRoundPolicy.isAssignable(round, ExamCandidateResultStatus.valueOf(resultStatus));
        var allowedOutcomes = editable
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
            taskItems(row.get(12, UUID.class)),
            criteria(row.get(11, UUID.class))
        ));
    }

    /**
     * Các phần thi của bài kèm dữ liệu để chấm. Số query cố định (4) bất kể bài có
     * bao nhiêu phần.
     *
     * <p>Hai lookup evaluation khác nhau là cố ý: điểm/nhận xét tham chiếu lấy từ
     * bản chấm đang có hiệu lực, còn lượt nói phải lấy từ bản AI vì chỉ bản AI mới
     * sinh turn.
     */
    private List<GradingTaskItemInfo> taskItems(UUID sessionId) {
        var rows = em.createQuery("""
            SELECT r.id, r.paperItemId, sec.title
            FROM ExamItemResponseJpaEntity r
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = r.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE r.sessionId = :sessionId
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("sessionId", sessionId)
            .getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }

        var responseIds = rows.stream().map(row -> row.get(0, UUID.class)).filter(Objects::nonNull).toList();
        var currentEvaluations = currentEvaluationsByResponseIds(responseIds);
        var aiEvaluationIds = aiEvaluationIdsByResponseIds(responseIds);
        var scoresByEvaluation = criterionScoresByEvaluationIds(
            currentEvaluations.values().stream().map(evaluation -> evaluation.id()).toList());
        var turnsByEvaluation = turnsByEvaluationIds(List.copyOf(aiEvaluationIds.values()));

        var result = new ArrayList<GradingTaskItemInfo>();
        for (var row : rows) {
            var responseId = row.get(0, UUID.class);
            var current = currentEvaluations.get(responseId);
            var aiEvaluationId = aiEvaluationIds.get(responseId);
            result.add(new GradingTaskItemInfo(
                row.get(1, UUID.class),
                responseId,
                row.get(2, String.class),
                current == null ? null : current.itemScore(),
                current == null ? null : current.feedbackSummary(),
                current == null ? List.of() : scoresByEvaluation.getOrDefault(current.id(), List.of()),
                aiEvaluationId == null ? List.of() : turnsByEvaluation.getOrDefault(aiEvaluationId, List.of())
            ));
        }
        return result;
    }

    private record CurrentEvaluation(UUID id, BigDecimal itemScore, String feedbackSummary) {
    }

    /**
     * Bản chấm đang có hiệu lực của mỗi response — mốc giáo viên đối chiếu. Lần đầu
     * rơi vào bản AI AUTO_GRADED; sau khi đã chấm tay rơi vào bản HUMAN FINALIZED.
     *
     * <p>Bộ lọc {@code status IN ('AUTO_GRADED','FINALIZED')} là bắt buộc ở MỌI query
     * "evaluation mới nhất" — xem {@code AI/shared/context.md}.
     *
     * <p>Order rồi giữ dòng đầu, không dùng MAX(evaluatedAt): hai bản trùng mốc thời
     * gian sẽ khiến so sánh bằng trả về cả hai.
     */
    private Map<UUID, CurrentEvaluation> currentEvaluationsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ev.responseId, ev.id, ev.itemScore, ev.feedbackSummary
            FROM ExamItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        var map = new LinkedHashMap<UUID, CurrentEvaluation>();
        for (var row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), new CurrentEvaluation(
                row.get(1, UUID.class), row.get(2, BigDecimal.class), row.get(3, String.class)));
        }
        return map;
    }

    /**
     * Bản AI của mỗi response — nguồn DUY NHẤT của lượt nói (audio/transcript), vì
     * bản chấm tay không bao giờ sinh turn. Sau khi giáo viên nộp, bản AI mang
     * SUPERSEDED nên query này cố tình không lọc theo status.
     */
    private Map<UUID, UUID> aiEvaluationIdsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ev.responseId, ev.id FROM ExamItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        var map = new LinkedHashMap<UUID, UUID>();
        for (var row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, UUID.class));
        }
        return map;
    }

    /** Điểm tiêu chí của nhiều evaluation trong 1 query, group theo evaluationId. */
    private Map<UUID, List<GradingCriterionScoreInfo>> criterionScoresByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT cs.evaluationId, cs.rubricCriterionId, rc.code, rc.name, cs.finalScore, cs.rationale
            FROM ExamItemCriterionScoreJpaEntity cs
            JOIN RubricCriterionJpaEntity rc ON rc.id = cs.rubricCriterionId
            WHERE cs.evaluationId IN (:evaluationIds)
            ORDER BY rc.order ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        var map = new LinkedHashMap<UUID, List<GradingCriterionScoreInfo>>();
        for (var row : rows) {
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

    /** Lượt nói của nhiều evaluation trong 1 query, group theo evaluationId. */
    private Map<UUID, List<GradingTurnInfo>> turnsByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT t.evaluationId, t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl,
                   t.transcript, t.durationSeconds
            FROM ExamItemEvaluationTurnJpaEntity t
            WHERE t.evaluationId IN (:evaluationIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        var map = new LinkedHashMap<UUID, List<GradingTurnInfo>>();
        for (var row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new GradingTurnInfo(
                    row.get(1, UUID.class),
                    row.get(2, Integer.class),
                    row.get(3, String.class),
                    row.get(4, String.class),
                    row.get(5, String.class),
                    row.get(6, String.class),
                    row.get(7, Integer.class)
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
        var normalizedKeyword = normalizeKeyword(keyword);
        var rows = em.createQuery("""
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

        var teacherIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var loadByTeacher = assignedLoadByTeacherIds(teacherIds);

        var result = new ArrayList<AssignableTeacherInfo>();
        for (var row : rows) {
            var teacherId = row.get(0, UUID.class);
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
     * Tải = số phân công ĐANG MỞ, đếm qua {@code active_result_id} nên tự đúng cho cả
     * bốn vòng: một giáo viên vừa chấm lần đầu vừa hậu kiểm thì tải là tổng cả hai.
     */
    @Override
    public Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ga.teacherId, COUNT(ga)
            FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.teacherId IN (:teacherIds) AND ga.activeResultId IS NOT NULL
            GROUP BY ga.teacherId
        """, Tuple.class)
            .setParameter("teacherIds", teacherIds)
            .getResultList();
        var map = new HashMap<UUID, Long>();
        for (var row : rows) {
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
        return em.createQuery("""
            SELECT cr.id
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
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
            .setParameter("resultStatuses", resultStatuses)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();
    }

    /**
     * Độ tự tin THẤP NHẤT trong các phần của bài, kèm điểm và ngưỡng đạt. Lấy min vì
     * một phần AI đuối là đủ để cả bài đáng soi — không lấy trung bình để khỏi bị các
     * phần dễ kéo lên.
     */
    @Override
    public List<GradingRiskInfo> findRiskInfos(Collection<UUID> candidateResultIds) {
        if (candidateResultIds == null || candidateResultIds.isEmpty()) {
            return List.of();
        }
        var rows = em.createQuery("""
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

        var result = new ArrayList<GradingRiskInfo>();
        for (var row : rows) {
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
     * Các con số chặn việc chốt sổ: bài chờ chấm chưa ai nhận, bài chờ chấm đang có
     * người cầm, đơn phúc khảo chưa xong, và bài đã vô hiệu.
     *
     * <p>Hai query, cả hai đều aggregate: một câu {@code SUM(CASE WHEN …)} cho các con
     * số, một câu {@code LIMIT 20} cho danh sách bài chặn làm ví dụ. Bản trước kéo MỌI
     * dòng của kỳ thi về Java rồi đếm bằng vòng lặp — chấp nhận được ở quy mô hiện tại,
     * nhưng đây là aggregate thuần tuý nên không có lý do gì để dữ liệu rời khỏi DB.
     */
    @Override
    public BulkFinalizePreviewInfo previewBulkFinalize(UUID schoolId, UUID examId) {
        var counts = em.createQuery("""
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
            WHERE e.schoolId = :schoolId AND cr.examId = :examId
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("pendingReview", ExamCandidateResultStatus.PENDING_REVIEW.name())
            .setParameter("invalid", ExamCandidateResultStatus.INVALID.name())
            .getSingleResult();

        var total = intOf(counts.get(0, Long.class));
        var blocked = intOf(counts.get(5, Long.class));

        // Danh sách mẫu: UI chỉ cần vài ví dụ để admin nhận ra vấn đề, con số tổng đã
        // nằm ở các trường đếm ở trên. Không lấy khi chẳng có gì chặn.
        var blockingResultIds = blocked == 0 ? List.<UUID>of() : em.createQuery("""
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
     * Bảng điểm để xuất CSV. Hai bước: lấy dòng chính, rồi nạp vòng chấm gần nhất theo
     * lô — không lặp query theo từng học sinh.
     *
     * <p>Cột "ca thi" là {@code startDate} của lịch chứ không phải một cái tên:
     * {@code exam_schedules} không có cột tên, một ca được nhận diện bằng mốc bắt đầu
     * (bản trước chọn {@code sch.name} nên câu này ném {@code UnknownPathException}
     * mỗi lần xuất).
     */
    @Override
    public List<ExamScoreRowInfo> findScoreRows(UUID schoolId, UUID examId, UUID scheduleId) {
        var rows = em.createQuery("""
            SELECT cr.id, u.fullName, u.email, e.name, sch.startDate, cr.totalScore, rb.name,
                   cr.status, cr.releasedAt
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamScheduleJpaEntity sch ON sch.id = c.scheduleId
            LEFT JOIN RubricResultBandJpaEntity rb ON rb.id = cr.rubricResultBandId
            WHERE e.schoolId = :schoolId
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            ORDER BY u.fullName ASC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        var candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var classNames = classNamesByCandidateResultIds(candidateResultIds);
        var lastRounds = lastCompletedRoundByResultIds(candidateResultIds);

        var result = new ArrayList<ExamScoreRowInfo>();
        for (var row : rows) {
            var candidateResultId = row.get(0, UUID.class);
            var lastRound = lastRounds.get(candidateResultId);
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

    /** Vòng đã hoàn thành gần nhất của nhiều bài, 1 query; giữ dòng đầu mỗi bài. */
    private Map<UUID, LastRound> lastCompletedRoundByResultIds(List<UUID> candidateResultIds) {
        if (candidateResultIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
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
        var map = new LinkedHashMap<UUID, LastRound>();
        for (var row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), new LastRound(
                row.get(1, String.class), row.get(2, String.class), row.get(3, String.class)));
        }
        return map;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    }
}
