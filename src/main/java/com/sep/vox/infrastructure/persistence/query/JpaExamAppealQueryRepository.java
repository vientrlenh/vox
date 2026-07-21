package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AppealCriterionMetaInfo;
import com.sep.vox.application.query.dto.AppealCriterionScoreInfo;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealItemInfo;
import com.sep.vox.application.query.dto.AppealReviewerInfo;
import com.sep.vox.application.query.dto.AppealReviewerItemInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.dto.AppealTaskDetailInfo;
import com.sep.vox.application.query.dto.AppealTaskInfo;
import com.sep.vox.application.query.dto.AppealTurnInfo;
import com.sep.vox.application.query.repository.ExamAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaExamAppealQueryRepository implements ExamAppealQueryRepository {

    /** Trạng thái đơn còn đang xử lý — dùng để tính quá hạn (deadline chỉ có ý nghĩa ở đây). */
    private static final Set<String> IN_PROGRESS_STATUSES = Set.of("APPROVED", "GRADING", "COMPARING");

    @PersistenceContext
    private EntityManager em;

    // ---- danh sách đơn (school admin) --------------------------------------

    @Override
    public PageResult<AppealSummaryInfo> searchAppeals(
            UUID schoolId, String status, String keyword, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);
        var normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";

        // KHÔNG join sang phần thi ở đây: một đơn có N phần, join to-many sẽ nhân dòng
        // và làm sai phân trang (một đơn ăn nhiều slot) lẫn lệch với query COUNT bên dưới.
        var query = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE e.schoolId = :schoolId
            AND (:status IS NULL OR a.status = :status)
            AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(e.name) LIKE :keyword)
            ORDER BY a.requestedAt DESC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize);

        var rows = query.getResultList();
        var appealIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var countsByAppeal = reviewerCountsByAppealIds(appealIds);
        var classNamesByAppeal = classNamesByAppealIds(appealIds);
        var partLabelsByAppeal = partLabelsByAppealIds(appealIds);
        var now = OffsetDateTime.now();

        var content = new ArrayList<AppealSummaryInfo>();
        for (var row : rows) {
            var appealId = row.get(0, UUID.class);
            var counts = countsByAppeal.getOrDefault(appealId, new int[] { 0, 0 });
            var status0 = row.get(4, String.class);
            var deadline = row.get(6, OffsetDateTime.class);
            content.add(new AppealSummaryInfo(
                appealId,
                row.get(1, String.class),
                classNamesByAppeal.get(appealId),
                row.get(2, String.class),
                partLabelsByAppeal.getOrDefault(appealId, List.of()),
                row.get(3, BigDecimal.class),
                status0,
                row.get(5, OffsetDateTime.class),
                deadline,
                counts[0],
                counts[1],
                isOverdue(deadline, status0, now)
            ));
        }

        var total = em.createQuery("""
            SELECT COUNT(a) FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE e.schoolId = :schoolId
            AND (:status IS NULL OR a.status = :status)
            AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(e.name) LIKE :keyword)
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    /** Đếm giám khảo + số đã nộp cho nhiều đơn trong 1 query (tránh N+1 theo dòng). */
    private Map<UUID, int[]> reviewerCountsByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT r.appealId, COUNT(r), SUM(CASE WHEN r.status = :submitted THEN 1 ELSE 0 END)
            FROM ExamAppealReviewerJpaEntity r
            WHERE r.appealId IN (:appealIds)
            GROUP BY r.appealId
        """, Tuple.class)
            .setParameter("submitted", ExamAppealReviewerStatus.SUBMITTED.name())
            .setParameter("appealIds", appealIds)
            .getResultList();
        var map = new HashMap<UUID, int[]>();
        for (var row : rows) {
            var total = row.get(1, Long.class);
            var done = row.get(2, Long.class);
            map.put(row.get(0, UUID.class), new int[] {
                total == null ? 0 : total.intValue(),
                done == null ? 0 : done.intValue()
            });
        }
        return map;
    }

    /** Tên lớp (đang hoạt động) của nhiều đơn trong 1 query; giữ giá trị đầu tiên mỗi đơn. */
    private Map<UUID, String> classNamesByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT a.id, sc.name
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN SchoolClassUserJpaEntity scu ON scu.userId = c.studentId AND scu.isActive = true
            JOIN SchoolClassJpaEntity sc ON sc.id = scu.schoolClassId
            WHERE a.id IN (:appealIds)
        """, Tuple.class)
            .setParameter("appealIds", appealIds)
            .getResultList();
        var map = new HashMap<UUID, String>();
        for (var row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, String.class));
        }
        return map;
    }

    private String className(UUID appealId) {
        return classNamesByAppealIds(List.of(appealId)).get(appealId);
    }

    /**
     * Nhãn phần thi của nhiều đơn trong 1 query — đơn có N phần nên tuyệt đối không
     * được join vào query phân trang. Nhiều phần cùng một section thì nhãn trùng, khử bằng set.
     */
    private Map<UUID, List<String>> partLabelsByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ai.appealId, sec.title
            FROM ExamResultAppealItemJpaEntity ai
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = ai.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE ai.appealId IN (:appealIds)
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("appealIds", appealIds)
            .getResultList();
        var map = new LinkedHashMap<UUID, LinkedHashSet<String>>();
        for (var row : rows) {
            var title = row.get(1, String.class);
            if (title != null) {
                map.computeIfAbsent(row.get(0, UUID.class), ignored -> new LinkedHashSet<>()).add(title);
            }
        }
        var result = new LinkedHashMap<UUID, List<String>>();
        map.forEach((appealId, titles) -> result.put(appealId, List.copyOf(titles)));
        return result;
    }

    // ---- stat cards --------------------------------------------------------

    @Override
    public AppealStatsInfo countByStatus(UUID schoolId) {
        var rows = em.createQuery("""
            SELECT a.status, COUNT(a)
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE e.schoolId = :schoolId
            GROUP BY a.status
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .getResultList();

        var pending = 0;
        var processing = 0;
        var published = 0;
        var rejected = 0;
        for (var row : rows) {
            var status = row.get(0, String.class);
            var count = row.get(1, Long.class).intValue();
            switch (status) {
                case "PENDING" -> pending += count;
                case "APPROVED", "GRADING", "COMPARING" -> processing += count;
                case "PUBLISHED" -> published += count;
                case "REJECTED" -> rejected += count;
                default -> { }
            }
        }
        return new AppealStatsInfo(pending, processing, published, rejected);
    }

    // ---- chi tiết đơn (school admin) ---------------------------------------

    @Override
    public Optional<AppealDetailInfo> findDetailById(UUID appealId, UUID schoolId) {
        var rows = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline,
                   a.reason, a.notes, a.decisionNote, a.scoreAfter, a.approvedAt, a.resolvedAt,
                   rv.scoringScaleMin, rv.scoringScaleMax
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            JOIN RubricVersionJpaEntity rv ON rv.id = cr.rubricVersionId
            WHERE a.id = :appealId AND e.schoolId = :schoolId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("schoolId", schoolId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        var row = rows.get(0);
        var status = row.get(4, String.class);
        var deadline = row.get(6, OffsetDateTime.class);

        return Optional.of(new AppealDetailInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            className(appealId),
            row.get(2, String.class),
            row.get(3, BigDecimal.class),
            status,
            row.get(5, OffsetDateTime.class),
            deadline,
            row.get(7, String.class),
            row.get(8, String.class),
            row.get(9, String.class),
            row.get(10, BigDecimal.class),
            row.get(11, OffsetDateTime.class),
            row.get(12, OffsetDateTime.class),
            appealItems(appealId),
            reviewers(appealId, true),
            isOverdue(deadline, status, OffsetDateTime.now()),
            row.get(13, BigDecimal.class),
            row.get(14, BigDecimal.class)
        ));
    }

    /**
     * Các phần thi của đơn kèm dữ liệu để đối chiếu. Số query cố định (5) bất kể
     * đơn có bao nhiêu phần.
     *
     * <p>Hai lookup evaluation khác nhau là cố ý: điểm đối chiếu lấy từ bản chấm
     * đang có hiệu lực, còn lượt nói phải lấy từ bản AI vì chỉ bản AI mới có turn.
     */
    private List<AppealItemInfo> appealItems(UUID appealId) {
        var rows = em.createQuery("""
            SELECT ai.id, ai.paperItemId, ai.responseId, sec.title, ai.finalScore
            FROM ExamResultAppealItemJpaEntity ai
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = ai.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE ai.appealId = :appealId
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }

        var responseIds = rows.stream().map(row -> row.get(2, UUID.class)).filter(Objects::nonNull).toList();
        var baselineEvaluationIds = currentEvaluationIdsByResponseIds(responseIds);
        var aiEvaluationIds = aiEvaluationIdsByResponseIds(responseIds);
        var scoresByEvaluation = criterionScoresByEvaluationIds(List.copyOf(baselineEvaluationIds.values()));
        var turnsByEvaluation = turnsByEvaluationIds(List.copyOf(aiEvaluationIds.values()));

        var result = new ArrayList<AppealItemInfo>();
        for (var row : rows) {
            var responseId = row.get(2, UUID.class);
            var baselineEvaluationId = baselineEvaluationIds.get(responseId);
            var aiEvaluationId = aiEvaluationIds.get(responseId);
            result.add(new AppealItemInfo(
                row.get(0, UUID.class),
                row.get(1, UUID.class),
                row.get(3, String.class),
                baselineEvaluationId == null
                    ? List.of() : scoresByEvaluation.getOrDefault(baselineEvaluationId, List.of()),
                aiEvaluationId == null ? List.of() : turnsByEvaluation.getOrDefault(aiEvaluationId, List.of()),
                row.get(4, BigDecimal.class)
            ));
        }
        return result;
    }

    /**
     * Bản chấm đang có hiệu lực của mỗi response — mốc để giám khảo đối chiếu.
     * Vòng đầu rơi vào bản AI AUTO_GRADED; vòng sau rơi vào bản HUMAN FINALIZED của
     * vòng trước, khớp với originalScore hiển thị cạnh nó thay vì điểm AI đã lỗi thời.
     *
     * <p>Order rồi giữ dòng đầu mỗi response, không dùng MAX(evaluatedAt): hai bản
     * trùng mốc thời gian sẽ khiến so sánh bằng trả về cả hai.
     */
    private Map<UUID, UUID> currentEvaluationIdsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT ev.responseId, ev.id FROM ExamItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
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

    /**
     * Bản AI của mỗi response — nguồn DUY NHẤT của lượt nói (audio/transcript), vì bản
     * chấm tay không bao giờ sinh turn. Sau khi công bố phúc khảo, bản AI mang
     * SUPERSEDED nên query này cố tình không lọc theo status.
     *
     * <p>Điểm đối chiếu KHÔNG lấy ở đây — xem {@link #currentEvaluationIdsByResponseIds}.
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

    /** Điểm tiêu chí của nhiều evaluation trong 1 query, group theo evaluationId (tránh N+1). */
    private Map<UUID, List<AppealCriterionScoreInfo>> criterionScoresByEvaluationIds(List<UUID> evaluationIds) {
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
        var map = new LinkedHashMap<UUID, List<AppealCriterionScoreInfo>>();
        for (var row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new AppealCriterionScoreInfo(
                    row.get(1, UUID.class),
                    row.get(2, String.class),
                    row.get(3, String.class),
                    row.get(4, BigDecimal.class),
                    row.get(5, String.class)
                ));
        }
        return map;
    }

    /** Lượt nói của nhiều evaluation trong 1 query, group theo evaluationId (tránh N+1). */
    private Map<UUID, List<AppealTurnInfo>> turnsByEvaluationIds(List<UUID> evaluationIds) {
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
        var map = new LinkedHashMap<UUID, List<AppealTurnInfo>>();
        for (var row : rows) {
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new AppealTurnInfo(
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

    private List<AppealReviewerInfo> reviewers(UUID appealId, boolean includeScores) {
        var rows = em.createQuery("""
            SELECT r.id, r.reviewerId, u.fullName, r.status, r.assignedAt, r.submittedAt
            FROM ExamAppealReviewerJpaEntity r
            JOIN UserJpaEntity u ON u.id = r.reviewerId
            WHERE r.appealId = :appealId
            ORDER BY r.assignedAt ASC
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .getResultList();

        var itemsByReviewer = includeScores
            ? reviewerItemsByAppealId(appealId, null)
            : Map.<UUID, List<AppealReviewerItemInfo>>of();

        var result = new ArrayList<AppealReviewerInfo>();
        for (var row : rows) {
            var items = itemsByReviewer.getOrDefault(row.get(0, UUID.class), List.of());
            var status = row.get(3, String.class);
            result.add(new AppealReviewerInfo(
                row.get(1, UUID.class),
                row.get(2, String.class),
                status,
                ExamAppealReviewerStatus.SUBMITTED.name().equals(status),
                row.get(4, OffsetDateTime.class),
                row.get(5, OffsetDateTime.class),
                averageSuggestedScore(items),
                items
            ));
        }
        return result;
    }

    /**
     * Báo cáo theo từng phần thi, group theo dòng phân công. `reviewerId` khác null thì
     * chỉ lấy của đúng giám khảo đó — dùng cho màn chấm mù.
     */
    private Map<UUID, List<AppealReviewerItemInfo>> reviewerItemsByAppealId(UUID appealId, UUID reviewerId) {
        var rows = em.createQuery("""
            SELECT ri.appealReviewerId, ri.appealItemId, sec.title, ri.suggestedScore, ri.note, ri.evaluationId
            FROM ExamAppealReviewerItemJpaEntity ri
            JOIN ExamAppealReviewerJpaEntity r ON r.id = ri.appealReviewerId
            JOIN ExamResultAppealItemJpaEntity ai ON ai.id = ri.appealItemId
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = ai.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE r.appealId = :appealId
            AND (:reviewerId IS NULL OR r.reviewerId = :reviewerId)
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("reviewerId", reviewerId)
            .getResultList();
        if (rows.isEmpty()) {
            return Map.of();
        }

        var evaluationIds = rows.stream().map(row -> row.get(5, UUID.class)).filter(Objects::nonNull).toList();
        var scoresByEvaluation = criterionScoresByEvaluationIds(evaluationIds);

        var map = new LinkedHashMap<UUID, List<AppealReviewerItemInfo>>();
        for (var row : rows) {
            var evaluationId = row.get(5, UUID.class);
            map.computeIfAbsent(row.get(0, UUID.class), ignored -> new ArrayList<>())
                .add(new AppealReviewerItemInfo(
                    row.get(1, UUID.class),
                    row.get(2, String.class),
                    row.get(3, BigDecimal.class),
                    row.get(4, String.class),
                    evaluationId == null ? List.of() : scoresByEvaluation.getOrDefault(evaluationId, List.of())
                ));
        }
        return map;
    }

    /** Con số headline của màn đối chiếu: trung bình điểm đề xuất trên các phần thi. */
    private BigDecimal averageSuggestedScore(List<AppealReviewerItemInfo> items) {
        var scores = items.stream()
            .map(AppealReviewerItemInfo::suggestedScore)
            .filter(Objects::nonNull)
            .toList();
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    // ---- việc của giám khảo ------------------------------------------------

    @Override
    public PageResult<AppealTaskInfo> findTasksByReviewerId(UUID reviewerId, String status, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);

        // Cùng lý do như searchAppeals: không join to-many vào query phân trang.
        var rows = em.createQuery("""
            SELECT a.id, e.name, a.deadline, r.status
            FROM ExamAppealReviewerJpaEntity r
            JOIN ExamResultAppealJpaEntity a ON a.id = r.appealId
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            WHERE r.reviewerId = :reviewerId
            AND (:status IS NULL OR r.status = :status)
            ORDER BY a.deadline ASC NULLS LAST, r.assignedAt DESC
        """, Tuple.class)
            .setParameter("reviewerId", reviewerId)
            .setParameter("status", status)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var partLabelsByAppeal = partLabelsByAppealIds(
            rows.stream().map(row -> row.get(0, UUID.class)).toList());
        var now = OffsetDateTime.now();
        var content = new ArrayList<AppealTaskInfo>();
        for (var row : rows) {
            var appealId = row.get(0, UUID.class);
            var deadline = row.get(2, OffsetDateTime.class);
            var myStatus = row.get(3, String.class);
            var overdue = deadline != null && deadline.isBefore(now)
                && ExamAppealReviewerStatus.ASSIGNED.name().equals(myStatus);
            content.add(new AppealTaskInfo(
                appealId,
                row.get(1, String.class),
                partLabelsByAppeal.getOrDefault(appealId, List.of()),
                deadline,
                myStatus,
                overdue
            ));
        }

        var total = em.createQuery("""
            SELECT COUNT(r) FROM ExamAppealReviewerJpaEntity r
            WHERE r.reviewerId = :reviewerId
            AND (:status IS NULL OR r.status = :status)
        """, Long.class)
            .setParameter("reviewerId", reviewerId)
            .setParameter("status", status)
            .getSingleResult();

        var totalPages = (int) Math.ceil((double) total / normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    // ---- màn chấm lại của giám khảo ----------------------------------------

    @Override
    public Optional<AppealTaskDetailInfo> findTaskDetail(UUID appealId, UUID reviewerId) {
        // Chỉ giám khảo được phân công vào CHÍNH đơn này mới xem được.
        // ExamResultAccessService không phủ trường hợp này (giám khảo không phải exam member).
        // Query này chạy MỘT MÌNH và trước mọi query khác — nó chính là chốt phân quyền,
        // không được để query phần thi nào chạy trước khi nó trả về dòng.
        var rows = em.createQuery("""
            SELECT a.id, cr.rubricVersionId
            FROM ExamAppealReviewerJpaEntity r
            JOIN ExamResultAppealJpaEntity a ON a.id = r.appealId
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            WHERE a.id = :appealId AND r.reviewerId = :reviewerId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("reviewerId", reviewerId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        var row = rows.get(0);

        // Chỉ nạp báo cáo của chính giám khảo này (chấm mù: không đụng dữ liệu người khác).
        var myItems = reviewerItemsByAppealId(appealId, reviewerId).values().stream()
            .flatMap(List::stream)
            .toList();

        return Optional.of(new AppealTaskDetailInfo(
            row.get(0, UUID.class),
            appealItems(appealId),
            criteria(row.get(1, UUID.class)),
            myItems
        ));
    }

    private List<AppealCriterionMetaInfo> criteria(UUID rubricVersionId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AppealCriterionMetaInfo(
                rc.id, rc.code, rc.name, rc.description, rc.minScore, rc.maxScore)
            FROM RubricCriterionJpaEntity rc
            WHERE rc.rubricVersionId = :rubricVersionId
            ORDER BY rc.order ASC
        """, AppealCriterionMetaInfo.class)
            .setParameter("rubricVersionId", rubricVersionId)
            .getResultList();
    }

    // ---- picker phân công --------------------------------------------------

    @Override
    public List<AppealReviewerLiteInfo> findAssignableReviewers(UUID schoolId, String keyword) {
        var normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
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

        var reviewerIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        var loadByReviewer = assignedLoadByReviewerIds(reviewerIds);

        var result = new ArrayList<AppealReviewerLiteInfo>();
        for (var row : rows) {
            var reviewerId = row.get(0, UUID.class);
            result.add(new AppealReviewerLiteInfo(
                reviewerId, row.get(1, String.class), loadByReviewer.getOrDefault(reviewerId, 0L)));
        }
        return result;
    }

    /** Số việc ASSIGNED mỗi giám khảo đang giữ, cho nhiều người trong 1 query (tránh N+1). */
    private Map<UUID, Long> assignedLoadByReviewerIds(List<UUID> reviewerIds) {
        if (reviewerIds.isEmpty()) {
            return Map.of();
        }
        var rows = em.createQuery("""
            SELECT r.reviewerId, COUNT(r)
            FROM ExamAppealReviewerJpaEntity r
            WHERE r.reviewerId IN (:reviewerIds) AND r.status = :assigned
            GROUP BY r.reviewerId
        """, Tuple.class)
            .setParameter("reviewerIds", reviewerIds)
            .setParameter("assigned", ExamAppealReviewerStatus.ASSIGNED.name())
            .getResultList();
        var map = new HashMap<UUID, Long>();
        for (var row : rows) {
            map.put(row.get(0, UUID.class), row.get(1, Long.class));
        }
        return map;
    }

    private boolean isOverdue(OffsetDateTime deadline, String status, OffsetDateTime now) {
        return deadline != null && deadline.isBefore(now) && IN_PROGRESS_STATUSES.contains(status);
    }
}
