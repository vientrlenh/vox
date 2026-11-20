package com.sep.vox.infrastructure.persistence.query;

import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AppealCriterionScoreInfo;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealItemInfo;
import com.sep.vox.application.query.dto.AppealReviewerInfo;
import com.sep.vox.application.query.dto.AppealReviewerLiteInfo;
import com.sep.vox.application.query.dto.AppealStatsInfo;
import com.sep.vox.application.query.dto.AppealSummaryInfo;
import com.sep.vox.application.query.dto.AppealTurnInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaAssessmentAppealQueryRepository implements AssessmentAppealQueryRepository {

    /** Trạng thái đơn còn đang xử lý — deadline chỉ có ý nghĩa ở đây. */
    private static final Set<String> IN_PROGRESS_STATUSES = Set.of("APPROVED", "GRADING");

    @PersistenceContext
    private EntityManager em;

    // ---- danh sách đơn (school admin) --------------------------------------

    @Override
    public PageResult<AppealSummaryInfo> searchAppeals(
            UUID schoolId, UUID assessmentId, String status, String keyword, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";

        // Do NOT join to the assessment slots here: one appeal has N slots, and a to-many join would multiply rows
        // and break pagination (one appeal taking several slots) as well as drift from the COUNT query below.
        TypedQuery<Tuple> query = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE e.schoolId = :schoolId
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:status IS NULL OR a.status = :status)
            AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(e.name) LIKE :keyword)
            ORDER BY a.requestedAt DESC
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size);

        List<Tuple> rows = query.getResultList();
        List<UUID> appealIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, AppealReviewerInfo> reviewersByAppeal = reviewersByAppealIds(appealIds);
        Map<UUID, String> classNamesByAppeal = classNamesByAppealIds(appealIds);
        Map<UUID, List<String>> partLabelsByAppeal = partLabelsByAppealIds(appealIds);
        Instant now = Instant.now();

        List<AppealSummaryInfo> content = new ArrayList<AppealSummaryInfo>();
        for (Tuple row : rows) {
            UUID appealId = row.get(0, UUID.class);
            AppealReviewerInfo reviewer = reviewersByAppeal.get(appealId);
            String appealStatus = row.get(4, String.class);
            Instant deadline = row.get(6, Instant.class);
            content.add(new AppealSummaryInfo(
                appealId,
                row.get(1, String.class),
                classNamesByAppeal.get(appealId),
                row.get(2, String.class),
                partLabelsByAppeal.getOrDefault(appealId, List.of()),
                row.get(3, BigDecimal.class),
                appealStatus,
                row.get(5, Instant.class),
                deadline,
                reviewer == null ? null : reviewer.reviewerName(),
                reviewer == null ? null : reviewer.status(),
                isOverdue(deadline, appealStatus, now)
            ));
        }

        Long total = em.createQuery("""
            SELECT COUNT(a) FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE e.schoolId = :schoolId
            AND (:assessmentId IS NULL OR cr.assessmentId = :assessmentId)
            AND (:status IS NULL OR a.status = :status)
            AND (:keyword IS NULL OR LOWER(u.fullName) LIKE :keyword OR LOWER(e.name) LIKE :keyword)
        """, Long.class)
            .setParameter("schoolId", schoolId)
            .setParameter("assessmentId", assessmentId)
            .setParameter("status", status)
            .setParameter("keyword", normalizedKeyword)
            .getSingleResult();

        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    /**
     * The graders of many appeals in 1 query. The source is the APPEAL round of {@code assessment_grading_assignments}
     * — the {@code assessment_appeal_reviewers} table has been dropped.
     *
     * <p>One appeal can have several rows over time (the first grader returns it, the admin assigns
     * someone else), so the NEWEST row is kept.
     */
    @Override
    public PageResult<AppealSummaryInfo> searchAppealsByStudentId(
            UUID studentId, String status, int page, int size) {
        List<Tuple> rows = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            WHERE c.studentId = :studentId
            AND (:status IS NULL OR a.status = :status)
            ORDER BY a.requestedAt DESC
        """, Tuple.class)
            .setParameter("studentId", studentId)
            .setParameter("status", status)
            .setFirstResult((page - 1) * size)
            .setMaxResults(size)
            .getResultList();

        List<UUID> appealIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, AppealReviewerInfo> reviewersByAppeal = reviewersByAppealIds(appealIds);
        Map<UUID, String> classNamesByAppeal = classNamesByAppealIds(appealIds);
        Map<UUID, List<String>> partLabelsByAppeal = partLabelsByAppealIds(appealIds);
        Instant now = Instant.now();
        List<AppealSummaryInfo> content = new ArrayList<AppealSummaryInfo>();
        for (Tuple row : rows) {
            UUID appealId = row.get(0, UUID.class);
            AppealReviewerInfo reviewer = reviewersByAppeal.get(appealId);
            String appealStatus = row.get(4, String.class);
            Instant deadline = row.get(6, Instant.class);
            content.add(new AppealSummaryInfo(
                appealId,
                row.get(1, String.class),
                classNamesByAppeal.get(appealId),
                row.get(2, String.class),
                partLabelsByAppeal.getOrDefault(appealId, List.of()),
                row.get(3, BigDecimal.class),
                appealStatus,
                row.get(5, Instant.class),
                deadline,
                reviewer == null ? null : reviewer.reviewerName(),
                reviewer == null ? null : reviewer.status(),
                isOverdue(deadline, appealStatus, now)
            ));
        }

        Long total = em.createQuery("""
            SELECT COUNT(a) FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            WHERE c.studentId = :studentId
            AND (:status IS NULL OR a.status = :status)
        """, Long.class)
            .setParameter("studentId", studentId)
            .setParameter("status", status)
            .getSingleResult();
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private Map<UUID, AppealReviewerInfo> reviewersByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ga.appealId, ga.id, ga.teacherId, u.fullName, ga.status, ga.outcome,
                   ga.assignedAt, ga.completedAt, ga.deadlineAt
            FROM AssessmentGradingAssignmentJpaEntity ga
            LEFT JOIN UserJpaEntity u ON u.id = ga.teacherId
            WHERE ga.appealId IN (:appealIds)
            ORDER BY ga.assignedAt DESC
        """, Tuple.class)
            .setParameter("appealIds", appealIds)
            .getResultList();

        Instant now = Instant.now();
        Map<UUID, AppealReviewerInfo> map = new LinkedHashMap<UUID, AppealReviewerInfo>();
        for (Tuple row : rows) {
            String assignmentStatus = row.get(4, String.class);
            Instant deadlineAt = row.get(8, Instant.class);
            map.putIfAbsent(row.get(0, UUID.class), new AppealReviewerInfo(
                row.get(1, UUID.class),
                row.get(2, UUID.class),
                row.get(3, String.class),
                assignmentStatus,
                row.get(5, String.class),
                row.get(6, Instant.class),
                row.get(7, Instant.class),
                deadlineAt,
                GradingAssignmentStatus.ASSIGNED.name().equals(assignmentStatus)
                    && deadlineAt != null && deadlineAt.isBefore(now)
            ));
        }
        return map;
    }

    /** The (active) class names of many appeals in 1 query; keeps the first value per appeal. */
    private Map<UUID, String> classNamesByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT a.id, sc.name
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN SchoolClassUserJpaEntity scu ON scu.userId = c.studentId AND scu.isActive = true
            JOIN SchoolClassJpaEntity sc ON sc.id = scu.schoolClassId
            WHERE a.id IN (:appealIds)
        """, Tuple.class)
            .setParameter("appealIds", appealIds)
            .getResultList();
        Map<UUID, String> map = new HashMap<UUID, String>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, String.class));
        }
        return map;
    }

    private String className(UUID appealId) {
        return classNamesByAppealIds(List.of(appealId)).get(appealId);
    }

    /**
     * The assessment-part labels of many appeals in 1 query — an appeal has N parts, so it must never
     * be joined into the paginated query. Several parts in the same section give duplicate labels, removed with a set.
     */
    private Map<UUID, List<String>> partLabelsByAppealIds(List<UUID> appealIds) {
        if (appealIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ai.appealId, sec.title
            FROM AssessmentResultAppealItemJpaEntity ai
            LEFT JOIN AssessmentPaperItemJpaEntity pi ON pi.id = ai.paperItemId
            LEFT JOIN AssessmentPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE ai.appealId IN (:appealIds)
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("appealIds", appealIds)
            .getResultList();
        Map<UUID, LinkedHashSet<String>> map = new LinkedHashMap<UUID, LinkedHashSet<String>>();
        for (Tuple row : rows) {
            String title = row.get(1, String.class);
            if (title != null) {
                map.computeIfAbsent(row.get(0, UUID.class), ignored -> new LinkedHashSet<>()).add(title);
            }
        }
        Map<UUID, List<String>> result = new LinkedHashMap<UUID, List<String>>();
        map.forEach((appealId, titles) -> result.put(appealId, List.copyOf(titles)));
        return result;
    }

    // ---- stat cards --------------------------------------------------------

    @Override
    public AppealStatsInfo countByStatus(UUID schoolId) {
        List<Tuple> rows = em.createQuery("""
            SELECT a.status, COUNT(a)
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            WHERE e.schoolId = :schoolId
            GROUP BY a.status
        """, Tuple.class)
            .setParameter("schoolId", schoolId)
            .getResultList();

        int pending = 0;
        int processing = 0;
        int published = 0;
        int rejected = 0;
        int withdrawn = 0;
        for (Tuple row : rows) {
            String status = row.get(0, String.class);
            int count = row.get(1, Long.class).intValue();
            switch (status) {
                case "PENDING" -> pending += count;
                case "APPROVED", "GRADING" -> processing += count;
                case "PUBLISHED" -> published += count;
                case "REJECTED" -> rejected += count;
                case "WITHDRAWN" -> withdrawn += count;
                default -> { }
            }
        }
        return new AppealStatsInfo(pending, processing, published, rejected, withdrawn);
    }

    // ---- chi tiết đơn (school admin) ---------------------------------------

    @Override
    public Optional<AppealDetailInfo> findDetailById(UUID appealId, UUID schoolId) {
        List<Tuple> rows = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline,
                   a.reason, a.notes, a.decisionNote, a.scoreAfter, a.approvedAt, a.resolvedAt,
                   rv.scoringScaleMin, rv.scoringScaleMax, a.withdrawnAt, a.reviewerOverrideReason
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
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
        Tuple row = rows.get(0);
        String status = row.get(4, String.class);
        Instant deadline = row.get(6, Instant.class);

        return Optional.of(new AppealDetailInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            className(appealId),
            row.get(2, String.class),
            row.get(3, BigDecimal.class),
            status,
            row.get(5, Instant.class),
            deadline,
            row.get(7, String.class),
            row.get(8, String.class),
            row.get(9, String.class),
            row.get(10, BigDecimal.class),
            row.get(11, Instant.class),
            row.get(12, Instant.class),
            row.get(15, Instant.class),
            row.get(16, String.class),
            appealItems(appealId),
            reviewersByAppealIds(List.of(appealId)).get(appealId),
            isOverdue(deadline, status, Instant.now()),
            row.get(13, BigDecimal.class),
            row.get(14, BigDecimal.class)
        ));
    }

    /**
     * The assessment parts of the appeal with the data needed for comparison. The number of queries is fixed (5) no matter
     * how many parts the appeal has.
     *
     * <p>The two different evaluation lookups are intentional: the comparison score comes from the evaluation
     * currently in effect, while the spoken turns must come from the AI version because only the AI version has turns.
     */
    @Override
    public Optional<AppealDetailInfo> findStudentDetailById(UUID appealId, UUID studentId) {
        List<Tuple> rows = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline,
                   a.reason, a.notes, a.decisionNote, a.scoreAfter, a.approvedAt, a.resolvedAt,
                   rv.scoringScaleMin, rv.scoringScaleMax, a.withdrawnAt, a.reviewerOverrideReason
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentJpaEntity e ON e.id = cr.assessmentId
            JOIN AssessmentCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            JOIN RubricVersionJpaEntity rv ON rv.id = cr.rubricVersionId
            WHERE a.id = :appealId AND c.studentId = :studentId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("studentId", studentId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Tuple row = rows.get(0);
        String status = row.get(4, String.class);
        Instant deadline = row.get(6, Instant.class);
        return Optional.of(new AppealDetailInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            className(appealId),
            row.get(2, String.class),
            row.get(3, BigDecimal.class),
            status,
            row.get(5, Instant.class),
            deadline,
            row.get(7, String.class),
            row.get(8, String.class),
            row.get(9, String.class),
            row.get(10, BigDecimal.class),
            row.get(11, Instant.class),
            row.get(12, Instant.class),
            row.get(15, Instant.class),
            row.get(16, String.class),
            appealItems(appealId),
            reviewersByAppealIds(List.of(appealId)).get(appealId),
            isOverdue(deadline, status, Instant.now()),
            row.get(13, BigDecimal.class),
            row.get(14, BigDecimal.class)
        ));
    }

    private List<AppealItemInfo> appealItems(UUID appealId) {
        List<Tuple> rows = em.createQuery("""
            SELECT ai.id, ai.paperItemId, ai.responseId, sec.title, ai.finalScore
            FROM AssessmentResultAppealItemJpaEntity ai
            LEFT JOIN AssessmentPaperItemJpaEntity pi ON pi.id = ai.paperItemId
            LEFT JOIN AssessmentPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE ai.appealId = :appealId
            ORDER BY sec.order ASC, pi.order ASC
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .getResultList();
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> responseIds = rows.stream().map(row -> row.get(2, UUID.class)).filter(Objects::nonNull).toList();
        Map<UUID, UUID> baselineEvaluationIds = currentEvaluationIdsByResponseIds(responseIds);
        Map<UUID, UUID> aiEvaluationIds = aiEvaluationIdsByResponseIds(responseIds);
        Map<UUID, List<AppealCriterionScoreInfo>> scoresByEvaluation = criterionScoresByEvaluationIds(List.copyOf(baselineEvaluationIds.values()));
        Map<UUID, List<AppealTurnInfo>> turnsByEvaluation = turnsByEvaluationIds(List.copyOf(aiEvaluationIds.values()));

        List<AppealItemInfo> result = new ArrayList<AppealItemInfo>();
        for (Tuple row : rows) {
            UUID responseId = row.get(2, UUID.class);
            UUID baselineEvaluationId = baselineEvaluationIds.get(responseId);
            UUID aiEvaluationId = aiEvaluationIds.get(responseId);
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
     * The evaluation currently in effect for each response — the reference point for comparison. The first round falls on
     * the AI AUTO_GRADED version; later rounds fall on the HUMAN FINALIZED version of the previous round, matching
     * the originalScore shown next to it instead of the outdated AI score.
     *
     * <p>Order then keep the first row per response, do not use MAX(evaluatedAt): two versions
     * with the same timestamp would make an equality comparison return both.
     */
    private Map<UUID, UUID> currentEvaluationIdsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ev.responseId, ev.id FROM AssessmentItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.status IN ('AUTO_GRADED', 'FINALIZED')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, UUID> map = new LinkedHashMap<UUID, UUID>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, UUID.class));
        }
        return map;
    }

    /**
     * The AI version of each response — the ONLY source of spoken turns (audio/transcript), because a
     * manual evaluation never produces turns. After the appeal is published the AI version carries
     * SUPERSEDED, so this query deliberately does not filter by status.
     *
     * <p>The comparison score is NOT taken here — see {@link #currentEvaluationIdsByResponseIds}.
     */
    private Map<UUID, UUID> aiEvaluationIdsByResponseIds(List<UUID> responseIds) {
        if (responseIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT ev.responseId, ev.id FROM AssessmentItemEvaluationJpaEntity ev
            WHERE ev.responseId IN (:responseIds)
            AND ev.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
            ORDER BY ev.responseId ASC, ev.evaluatedAt DESC
        """, Tuple.class)
            .setParameter("responseIds", responseIds)
            .getResultList();
        Map<UUID, UUID> map = new LinkedHashMap<UUID, UUID>();
        for (Tuple row : rows) {
            map.putIfAbsent(row.get(0, UUID.class), row.get(1, UUID.class));
        }
        return map;
    }

    /** Criterion scores of many evaluations in 1 query, grouped by evaluationId (avoids N+1). */
    private Map<UUID, List<AppealCriterionScoreInfo>> criterionScoresByEvaluationIds(List<UUID> evaluationIds) {
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
        Map<UUID, List<AppealCriterionScoreInfo>> map = new LinkedHashMap<UUID, List<AppealCriterionScoreInfo>>();
        for (Tuple row : rows) {
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

    /** Spoken turns of many evaluations in 1 query, grouped by evaluationId (avoids N+1). */
    private Map<UUID, List<AppealTurnInfo>> turnsByEvaluationIds(List<UUID> evaluationIds) {
        if (evaluationIds.isEmpty()) {
            return Map.of();
        }
        List<Tuple> rows = em.createQuery("""
            SELECT t.evaluationId, t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl,
                   t.transcript, t.durationSeconds
            FROM AssessmentItemEvaluationTurnJpaEntity t
            WHERE t.evaluationId IN (:evaluationIds)
            ORDER BY t.turnOrder ASC
        """, Tuple.class)
            .setParameter("evaluationIds", evaluationIds)
            .getResultList();
        Map<UUID, List<AppealTurnInfo>> map = new LinkedHashMap<UUID, List<AppealTurnInfo>>();
        for (Tuple row : rows) {
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

    // ---- picker phân công --------------------------------------------------

    /**
     * Candidates for grading the appeal, with a conflict-of-interest flag.
     *
     * <p>Conflicted people are NOT filtered out, only flagged: silently filtering would leave the admin of a
     * small school looking at an empty list without understanding why, and they still need to be able to override.
     */
    @Override
    public List<AppealReviewerLiteInfo> findAssignableReviewers(UUID schoolId, UUID appealId, String keyword) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";
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

        List<UUID> reviewerIds = rows.stream().map(row -> row.get(0, UUID.class)).toList();
        Map<UUID, Long> loadByReviewer = openLoadByTeacherIds(reviewerIds);
        Set<UUID> conflicted = appealId == null ? Set.<UUID>of() : teacherIdsWithHumanEvaluationForAppeal(appealId);

        List<AppealReviewerLiteInfo> result = new ArrayList<AppealReviewerLiteInfo>();
        for (Tuple row : rows) {
            UUID reviewerId = row.get(0, UUID.class);
            result.add(new AppealReviewerLiteInfo(
                reviewerId,
                row.get(1, String.class),
                loadByReviewer.getOrDefault(reviewerId, 0L),
                conflicted.contains(reviewerId)
            ));
        }
        return result;
    }

    /**
     * Ai đã từng ghi điểm tay cho bài của đơn này. Người chấm tay nằm ở
     * {@code reviewerId} của evaluation engine HUMAN; người chỉ xác nhận điểm cũ
     * (UPHOLD) không sinh evaluation nên không bị tính là xung đột.
     */
    private Set<UUID> teacherIdsWithHumanEvaluationForAppeal(UUID appealId) {
        return new HashSet<>(em.createQuery("""
            SELECT DISTINCT ev.reviewerId
            FROM AssessmentResultAppealJpaEntity a
            JOIN AssessmentCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN AssessmentItemResponseJpaEntity r ON r.sessionId = cr.sessionId
            JOIN AssessmentItemEvaluationJpaEntity ev ON ev.responseId = r.id
            WHERE a.id = :appealId
            AND ev.engineType = 'HUMAN'
            AND ev.reviewerId IS NOT NULL
        """, UUID.class)
            .setParameter("appealId", appealId)
            .getResultList());
    }

    /** Load = the number of open assignments, across all four rounds — the same formula as the grading screen. */
    private Map<UUID, Long> openLoadByTeacherIds(List<UUID> teacherIds) {
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

    private boolean isOverdue(Instant deadline, String status, Instant now) {
        return deadline != null && deadline.isBefore(now) && IN_PROGRESS_STATUSES.contains(status);
    }
}