package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AppealCriterionMetaInfo;
import com.sep.vox.application.query.dto.AppealCriterionScoreInfo;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.dto.AppealReviewerInfo;
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

    @PersistenceContext
    private EntityManager em;

    // ---- danh sách đơn (school admin) --------------------------------------

    @Override
    public PageResult<AppealSummaryInfo> searchAppeals(
            UUID schoolId, String status, String keyword, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);
        var normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim().toLowerCase() + "%";

        var query = em.createQuery("""
            SELECT a.id, u.fullName, e.name, a.scoreBefore, a.status, a.requestedAt, a.deadline,
                   sec.title, a.paperItemId
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = a.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
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
        var content = new ArrayList<AppealSummaryInfo>();
        for (var row : rows) {
            var appealId = row.get(0, UUID.class);
            var counts = reviewerCounts(appealId);
            content.add(new AppealSummaryInfo(
                appealId,
                row.get(1, String.class),
                className(row.get(0, UUID.class)),
                row.get(2, String.class),
                row.get(7, String.class),
                row.get(3, BigDecimal.class),
                row.get(4, String.class),
                row.get(5, OffsetDateTime.class),
                row.get(6, OffsetDateTime.class),
                counts[0],
                counts[1]
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

    private int[] reviewerCounts(UUID appealId) {
        var row = em.createQuery("""
            SELECT COUNT(r), SUM(CASE WHEN r.status = :submitted THEN 1 ELSE 0 END)
            FROM ExamAppealReviewerJpaEntity r
            WHERE r.appealId = :appealId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("submitted", ExamAppealReviewerStatus.SUBMITTED.name())
            .getSingleResult();
        var total = row.get(0, Long.class);
        var done = row.get(1, Long.class);
        return new int[] { total == null ? 0 : total.intValue(), done == null ? 0 : done.intValue() };
    }

    private String className(UUID appealId) {
        var rows = em.createQuery("""
            SELECT sc.name
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN SchoolClassUserJpaEntity scu ON scu.userId = c.studentId AND scu.isActive = true
            JOIN SchoolClassJpaEntity sc ON sc.id = scu.schoolClassId
            WHERE a.id = :appealId
        """, String.class)
            .setParameter("appealId", appealId)
            .setMaxResults(1)
            .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
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
                   sec.title, a.responseId, cr.rubricVersionId
            FROM ExamResultAppealJpaEntity a
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            JOIN ExamCandidateJpaEntity c ON c.id = cr.candidateId
            JOIN UserJpaEntity u ON u.id = c.studentId
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = a.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE a.id = :appealId AND e.schoolId = :schoolId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("schoolId", schoolId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        var row = rows.get(0);
        var responseId = row.get(14, UUID.class);
        var aiEvaluationId = findAiEvaluationId(responseId);

        return Optional.of(new AppealDetailInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            className(appealId),
            row.get(2, String.class),
            row.get(13, String.class),
            row.get(3, BigDecimal.class),
            row.get(4, String.class),
            row.get(5, OffsetDateTime.class),
            row.get(6, OffsetDateTime.class),
            row.get(7, String.class),
            row.get(8, String.class),
            row.get(9, String.class),
            row.get(10, BigDecimal.class),
            row.get(11, OffsetDateTime.class),
            row.get(12, OffsetDateTime.class),
            aiEvaluationId == null ? List.of() : criterionScores(aiEvaluationId),
            aiEvaluationId == null ? List.of() : turns(aiEvaluationId),
            reviewers(appealId, true)
        ));
    }

    /**
     * Điểm AI gốc để đối chiếu. Sau khi công bố phúc khảo, bản AI mang SUPERSEDED —
     * vẫn phải trả về, vì đây chính là mốc so sánh của màn đối chiếu.
     */
    private UUID findAiEvaluationId(UUID responseId) {
        if (responseId == null) {
            return null;
        }
        var rows = em.createQuery("""
            SELECT ev.id FROM ExamItemEvaluationJpaEntity ev
            WHERE ev.responseId = :responseId
            AND ev.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
            ORDER BY ev.evaluatedAt DESC
        """, UUID.class)
            .setParameter("responseId", responseId)
            .setMaxResults(1)
            .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<AppealCriterionScoreInfo> criterionScores(UUID evaluationId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AppealCriterionScoreInfo(
                cs.rubricCriterionId, rc.code, rc.name, cs.finalScore, cs.rationale)
            FROM ExamItemCriterionScoreJpaEntity cs
            JOIN RubricCriterionJpaEntity rc ON rc.id = cs.rubricCriterionId
            WHERE cs.evaluationId = :evaluationId
            ORDER BY rc.order ASC
        """, AppealCriterionScoreInfo.class)
            .setParameter("evaluationId", evaluationId)
            .getResultList();
    }

    private List<AppealTurnInfo> turns(UUID evaluationId) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AppealTurnInfo(
                t.id, t.turnOrder, t.turnType, t.promptText, t.audioUrl, t.transcript, t.durationSeconds)
            FROM ExamItemEvaluationTurnJpaEntity t
            WHERE t.evaluationId = :evaluationId
            ORDER BY t.turnOrder ASC
        """, AppealTurnInfo.class)
            .setParameter("evaluationId", evaluationId)
            .getResultList();
    }

    private List<AppealReviewerInfo> reviewers(UUID appealId, boolean includeScores) {
        var rows = em.createQuery("""
            SELECT r.reviewerId, u.fullName, r.status, r.assignedAt, r.submittedAt,
                   r.suggestedScore, r.note, r.evaluationId
            FROM ExamAppealReviewerJpaEntity r
            JOIN UserJpaEntity u ON u.id = r.reviewerId
            WHERE r.appealId = :appealId
            ORDER BY r.assignedAt ASC
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .getResultList();

        var result = new ArrayList<AppealReviewerInfo>();
        for (var row : rows) {
            var status = row.get(2, String.class);
            var evaluationId = row.get(7, UUID.class);
            result.add(new AppealReviewerInfo(
                row.get(0, UUID.class),
                row.get(1, String.class),
                status,
                ExamAppealReviewerStatus.SUBMITTED.name().equals(status),
                row.get(3, OffsetDateTime.class),
                row.get(4, OffsetDateTime.class),
                row.get(5, BigDecimal.class),
                row.get(6, String.class),
                includeScores && evaluationId != null ? criterionScores(evaluationId) : null
            ));
        }
        return result;
    }

    // ---- việc của giám khảo ------------------------------------------------

    @Override
    public PageResult<AppealTaskInfo> findTasksByReviewerId(UUID reviewerId, String status, int page, int size) {
        var normalizedPage = Math.max(page, 0);
        var normalizedSize = Math.max(size, 1);

        var content = em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.AppealTaskInfo(
                a.id, e.name, sec.title, a.deadline, r.status)
            FROM ExamAppealReviewerJpaEntity r
            JOIN ExamResultAppealJpaEntity a ON a.id = r.appealId
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            JOIN ExamJpaEntity e ON e.id = cr.examId
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = a.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE r.reviewerId = :reviewerId
            AND (:status IS NULL OR r.status = :status)
            ORDER BY a.deadline ASC NULLS LAST, r.assignedAt DESC
        """, AppealTaskInfo.class)
            .setParameter("reviewerId", reviewerId)
            .setParameter("status", status)
            .setFirstResult(normalizedPage * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

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
        var rows = em.createQuery("""
            SELECT a.id, sec.title, a.responseId, cr.rubricVersionId, r.evaluationId
            FROM ExamAppealReviewerJpaEntity r
            JOIN ExamResultAppealJpaEntity a ON a.id = r.appealId
            JOIN ExamCandidateResultJpaEntity cr ON cr.id = a.candidateResultId
            LEFT JOIN ExamPaperItemJpaEntity pi ON pi.id = a.paperItemId
            LEFT JOIN ExamPaperSectionJpaEntity sec ON sec.id = pi.sectionId
            WHERE a.id = :appealId AND r.reviewerId = :reviewerId
        """, Tuple.class)
            .setParameter("appealId", appealId)
            .setParameter("reviewerId", reviewerId)
            .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        var row = rows.get(0);
        var responseId = row.get(2, UUID.class);
        var aiEvaluationId = findAiEvaluationId(responseId);
        var myEvaluationId = row.get(4, UUID.class);

        var myReport = reviewers(appealId, true).stream()
            .filter(item -> item.reviewerId().equals(reviewerId))
            .filter(item -> myEvaluationId != null)
            .findFirst()
            .orElse(null);

        return Optional.of(new AppealTaskDetailInfo(
            row.get(0, UUID.class),
            row.get(1, String.class),
            aiEvaluationId == null ? List.of() : turns(aiEvaluationId),
            aiEvaluationId == null ? List.of() : criterionScores(aiEvaluationId),
            criteria(row.get(3, UUID.class)),
            myReport
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

        var result = new ArrayList<AppealReviewerLiteInfo>();
        for (var row : rows) {
            var reviewerId = row.get(0, UUID.class);
            var load = em.createQuery("""
                SELECT COUNT(r) FROM ExamAppealReviewerJpaEntity r
                WHERE r.reviewerId = :reviewerId AND r.status = :assigned
            """, Long.class)
                .setParameter("reviewerId", reviewerId)
                .setParameter("assigned", ExamAppealReviewerStatus.ASSIGNED.name())
                .getSingleResult();
            result.add(new AppealReviewerLiteInfo(reviewerId, row.get(1, String.class), load));
        }
        return result;
    }
}
