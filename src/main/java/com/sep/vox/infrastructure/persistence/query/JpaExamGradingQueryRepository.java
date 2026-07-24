package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.dto.GradingCriterionMetaInfo;
import com.sep.vox.application.query.dto.GradingCriterionScoreInfo;
import com.sep.vox.application.query.dto.GradingStatsInfo;
import com.sep.vox.application.query.dto.GradingTaskDetailInfo;
import com.sep.vox.application.query.dto.GradingTaskInfo;
import com.sep.vox.application.query.dto.GradingTaskItemInfo;
import com.sep.vox.application.query.dto.GradingTurnInfo;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.GradingAssignmentStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaExamGradingQueryRepository implements ExamGradingQueryRepository {

    /**
     * Chỉ bài PENDING_REVIEW mới nằm trong phạm vi chấm tay. Bài RELEASED đã công
     * bố, không chấm lại (QĐ #3) nên không được lọt vào bảng phân công lẫn thẻ số.
     */
    private static final String GRADABLE_STATUS = ExamCandidateResultStatus.PENDING_REVIEW.name();

    @PersistenceContext
    private EntityManager em;

    // ---- bảng phân công (school admin) --------------------------------------

    @Override
    public PageResult<GradingAssignmentRowInfo> searchAssignments(
            UUID schoolId, UUID examId, UUID scheduleId, UUID teacherId, String status, String keyword,
            int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);
        var normalizedKeyword = normalizeKeyword(keyword);

        // LEFT JOIN sang assignment là cố ý: bảng phải hiển thị CẢ bài chưa gán ai,
        // vì "gán tay cho dòng đang trống" là thao tác chính của màn này.
        var rows = em.createQuery("""
            SELECT cr.id, u.fullName, e.name, cr.status, s.flagged,
                   ga.id, ga.teacherId, t.fullName, ga.status, ga.assignedAt, ga.completedAt
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamGradingAssignmentJpaEntity ga ON ga.candidateResultId = cr.id
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND cr.status = :gradableStatus
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND (:teacherId IS NULL OR ga.teacherId = :teacherId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:keyword IS NULL OR LOWER(t.fullName) LIKE :keyword
                 OR LOWER(CAST(cr.id AS string)) LIKE :keyword)
            ORDER BY ga.assignedAt DESC NULLS FIRST, cr.id ASC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("gradableStatus", GRADABLE_STATUS)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var candidateResultIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var classNames = classNamesByCandidateResultIds(candidateResultIds);

        var content = new ArrayList<GradingAssignmentRowInfo>();
        for (var row : rows) {
            var candidateResultId = row.get(0, UUID.class);
            content.add(new GradingAssignmentRowInfo(
                candidateResultId,
                GradingResultCode.of(candidateResultId),
                row.get(1, String.class),
                classNames.get(candidateResultId),
                row.get(2, String.class),
                row.get(3, String.class),
                Boolean.TRUE.equals(row.get(4, Boolean.class)),
                row.get(5, UUID.class),
                row.get(6, UUID.class),
                row.get(7, String.class),
                row.get(8, String.class),
                row.get(9, OffsetDateTime.class),
                row.get(10, OffsetDateTime.class)
            ));
        }

        var total = em.createQuery("""
            SELECT COUNT(cr) FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamGradingAssignmentJpaEntity ga ON ga.candidateResultId = cr.id
            LEFT JOIN UserJpaEntity t ON t.id = ga.teacherId
            WHERE e.schoolId = :schoolId
            AND cr.status = :gradableStatus
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND (:teacherId IS NULL OR ga.teacherId = :teacherId)
            AND (:status IS NULL OR ga.status = :status)
            AND (:keyword IS NULL OR LOWER(t.fullName) LIKE :keyword
                 OR LOWER(CAST(cr.id AS string)) LIKE :keyword)
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("gradableStatus", GRADABLE_STATUS)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
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
    public GradingStatsInfo countByStatus(UUID schoolId, UUID examId, UUID scheduleId) {
        // Group theo trạng thái phân công, với NULL = chưa gán. Mẫu số luôn là
        // tổng bài PENDING_REVIEW, không phải tổng bài của kỳ thi.
        var rows = em.createQuery("""
            SELECT ga.status, COUNT(cr)
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            LEFT JOIN ExamGradingAssignmentJpaEntity ga ON ga.candidateResultId = cr.id
            WHERE e.schoolId = :schoolId
            AND cr.status = :gradableStatus
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            GROUP BY ga.status
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("gradableStatus", GRADABLE_STATUS)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();

        var unassigned = 0;
        var assigned = 0;
        // COMPLETED trên bài vẫn PENDING_REVIEW gần như không xảy ra sau khi /grade
        // bắt phủ đủ (chấm xong -> RELEASED, rời phạm vi). Vẫn cộng vào tổng để không
        // lệch nếu có trạng thái quá độ; không tách ra ô riêng.
        var completedButStillPending = 0;
        for (var row : rows) {
            var status = row.get(0, String.class);
            var count = row.get(1, Long.class).intValue();
            if (status == null) {
                unassigned += count;
            } else if (GradingAssignmentStatus.COMPLETED.name().equals(status)) {
                completedButStillPending += count;
            } else {
                assigned += count;
            }
        }
        return new GradingStatsInfo(unassigned + assigned + completedButStillPending, unassigned, assigned);
    }

    // ---- hàng đợi của giáo viên --------------------------------------------

    @Override
    public PageResult<GradingTaskInfo> findTasksByTeacherId(UUID teacherId, String status, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);

        // Không join sang candidate/user: giáo viên chấm ẩn danh, dữ liệu học sinh
        // không được rời khỏi read model này.
        var rows = em.createQuery("""
            SELECT ga.id, cr.id, e.name, ga.status, s.flagged, ga.assignedAt
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE ga.teacherId = :teacherId
            AND (:status IS NULL OR ga.status = :status)
            ORDER BY ga.status ASC, ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var partCounts = partCountsByCandidateResultIds(
            rows.stream().map(row -> row.get(1, UUID.class)).toList());

        var content = new ArrayList<GradingTaskInfo>();
        for (var row : rows) {
            var candidateResultId = row.get(1, UUID.class);
            content.add(new GradingTaskInfo(
                row.get(0, UUID.class),
                candidateResultId,
                GradingResultCode.of(candidateResultId),
                row.get(2, String.class),
                partCounts.getOrDefault(candidateResultId, 0),
                row.get(3, String.class),
                Boolean.TRUE.equals(row.get(4, Boolean.class)),
                row.get(5, OffsetDateTime.class)
            ));
        }

        var total = em.createQuery("""
            SELECT COUNT(ga) FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.teacherId = :teacherId
            AND (:status IS NULL OR ga.status = :status)
        """, Long.class)
            .setParameter("teacherId", teacherId)
            .setParameter("status", status)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
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
            SELECT ga.id, cr.id, e.name, ga.status, cr.status, s.flagged, s.flagReason,
                   cr.totalScore, cr.rubricVersionId, cr.sessionId
            FROM ExamGradingAssignmentJpaEntity ga
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = ga.candidateResultId
            JOIN ExamSessionJpaEntity s ON s.id = cr.sessionId
            JOIN ExamJpaEntity e ON e.id = cr.examId
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
        var assignmentStatus = row.get(3, String.class);
        var resultStatus = row.get(4, String.class);

        // Cờ chỉ-đọc do BE quyết, FE không tự suy: bài đã RELEASED hoặc phân công đã
        // chốt thì màn chấm chỉ để xem lại (QĐ #3).
        var editable = GRADABLE_STATUS.equals(resultStatus)
            && !GradingAssignmentStatus.COMPLETED.name().equals(assignmentStatus);

        return Optional.of(new GradingTaskDetailInfo(
            row.get(0, UUID.class),
            candidateResultId,
            GradingResultCode.of(candidateResultId),
            row.get(2, String.class),
            assignmentStatus,
            resultStatus,
            Boolean.TRUE.equals(row.get(5, Boolean.class)),
            row.get(6, String.class),
            row.get(7, BigDecimal.class),
            editable,
            taskItems(row.get(9, UUID.class)),
            criteria(row.get(8, UUID.class))
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
            currentEvaluations.values().stream().map(CurrentEvaluation::id).toList());
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

    /** Số bài ASSIGNED mỗi giáo viên đang giữ, batch trong 1 query (tránh N+1). */
    @Override
    public Map<UUID, Long> assignedLoadByTeacherIds(Collection<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ga.teacherId, COUNT(ga)
            FROM ExamGradingAssignmentJpaEntity ga
            WHERE ga.teacherId IN (:teacherIds) AND ga.status = :assigned
            GROUP BY ga.teacherId
        """, Tuple.class)
            .setParameter("teacherIds", teacherIds)
            .setParameter("assigned", GradingAssignmentStatus.ASSIGNED.name())
            .getResultList();
        var map = new HashMap<UUID, Long>();
        for (var row : rows) {
            map.put(row.get(0, UUID.class), row.get(1, Long.class));
        }
        return map;
    }

    // ---- auto-assign -------------------------------------------------------

    @Override
    public List<UUID> findUnassignedPendingReviewResultIds(UUID schoolId, UUID examId, UUID scheduleId) {
        // NOT EXISTS thay vì LEFT JOIN ... IS NULL: unique index trên
        // candidate_result_id nên nửa còn lại của join chỉ tốn công, và cách này
        // giữ auto-assign chạy lại được nhiều lần mà không gán trùng.
        return em.createQuery("""
            SELECT cr.id
            FROM ExamCandidateResultJpaEntity cr
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE e.schoolId = :schoolId
            AND cr.status = :gradableStatus
            AND (:examId IS NULL OR cr.examId = :examId)
            AND (:scheduleId IS NULL OR c.scheduleId = :scheduleId)
            AND NOT EXISTS (
                SELECT 1 FROM ExamGradingAssignmentJpaEntity ga WHERE ga.candidateResultId = cr.id
            )
            ORDER BY cr.id ASC
        """, UUID.class)
            .setParameter("schoolId", schoolId)
            .setParameter("gradableStatus", GRADABLE_STATUS)
            .setParameter("examId", examId)
            .setParameter("scheduleId", scheduleId)
            .getResultList();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
    }
}
