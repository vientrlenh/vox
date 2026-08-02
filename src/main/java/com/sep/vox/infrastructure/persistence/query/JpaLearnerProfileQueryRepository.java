package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.query.dto.LearnerProfileInfo;
import com.sep.vox.application.query.repository.LearnerProfileQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataLearnerProfileRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaLearnerProfileQueryRepository
        implements LearnerProfileQueryRepository {

    @PersistenceContext
    private EntityManager em;

    private final SpringDataLearnerProfileRepository repository;

    public JpaLearnerProfileQueryRepository(
            SpringDataLearnerProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public LearnerProfileInfo findCurrent(UUID studentId) {
        var current = repository
            .findTopByStudentIdOrderByVersionDesc(studentId)
            .orElse(null);
        var target = findTargetBand(studentId);
        if (target == null) {
            throw new NotFoundException(
                "Không tìm thấy chính sách đánh giá đang hiệu lực"
            );
        }
        var attainment = findAttainmentPercent(studentId);
        return new LearnerProfileInfo(
            current == null || current.getGoalType() == null
                ? "ABILITY_IMPROVEMENT"
                : current.getGoalType(),
            current == null || current.getFlsaScore() == null
                ? null
                : current.getFlsaScore().doubleValue(),
            (String) target[0],
            (String) target[1],
            attainment == null
                ? null
                : BigDecimal.valueOf(attainment)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue(),
            findEstimatedBandCode(studentId),
            current == null || current.isAutoUpdateInterest(),
            current == null || current.getQuizCompletedAt() == null
                ? null
                : current.getQuizCompletedAt().toString()
        );
    }

    private Object[] findTargetBand(UUID studentId) {
        try {
            return (Object[]) em.createNativeQuery("""
                SELECT band.code AS code, band.label AS label
                FROM assessment_policies policy
                JOIN framework_result_bands band ON band.id = policy.target_framework_band_id
                LEFT JOIN school_class_users membership
                  ON membership.user_id = :studentId
                 AND membership.is_active = true
                LEFT JOIN school_classes class ON class.id = membership.school_class_id
                WHERE policy.status = 'PUBLISHED'
                  AND policy.effective_from <= CURRENT_TIMESTAMP
                  AND (policy.effective_to IS NULL OR policy.effective_to >= CURRENT_TIMESTAMP)
                  AND (policy.school_id IS NULL OR policy.school_id = class.school_id)
                  AND (policy.school_grade_id IS NULL OR policy.school_grade_id = class.school_grade_id)
                  AND (policy.school_class_id IS NULL OR policy.school_class_id = class.id)
                ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                         (policy.school_grade_id IS NOT NULL) DESC,
                         (policy.school_id IS NOT NULL) DESC,
                         policy.version DESC
                LIMIT 1
                """)
                .setParameter("studentId", studentId)
                .getSingleResult();
        } catch (NoResultException exception) {
            return null;
        }
    }

    private Double findAttainmentPercent(UUID studentId) {
        return (Double) em.createNativeQuery("""
            SELECT AVG(
                (score.final_score - criterion.min_score)
                / NULLIF(criterion.max_score - criterion.min_score, 0)
            ) * 100
            FROM exam_item_criterion_scores score
            JOIN rubric_criterions criterion ON criterion.id = score.rubric_criterion_id
            JOIN exam_item_evaluations evaluation ON evaluation.id = score.evaluation_id
            JOIN exam_item_responses response ON response.id = evaluation.response_id
            JOIN exam_sessions session ON session.id = response.session_id
            JOIN exam_candidates candidate ON candidate.id = session.candidate_id
            WHERE candidate.student_id = :studentId
              AND candidate.blocked_at IS NULL
              AND evaluation.marked_invalid = false
            """)
            .setParameter("studentId", studentId)
            .getSingleResult();
    }

    private String findEstimatedBandCode(UUID studentId) {
        var rows = em.createNativeQuery("""
            WITH observations AS (
                SELECT band.code,
                       band.result_band_order,
                       COUNT(*) OVER () AS total
                FROM exam_item_criterion_scores score
                JOIN rubric_criterions criterion ON criterion.id = score.rubric_criterion_id
                JOIN framework_criteria framework
                  ON framework.id = criterion.framework_criterion_id
                JOIN framework_result_bands band
                  ON band.framework_version_id = framework.framework_version_id
                 AND band.code = score.matched_band_code
                JOIN exam_item_evaluations evaluation ON evaluation.id = score.evaluation_id
                JOIN exam_item_responses response ON response.id = evaluation.response_id
                JOIN exam_sessions session ON session.id = response.session_id
                JOIN exam_candidates candidate ON candidate.id = session.candidate_id
                WHERE candidate.student_id = :studentId
                  AND candidate.blocked_at IS NULL
                  AND evaluation.marked_invalid = false
                  AND score.matched_band_code IS NOT NULL
            )
            SELECT code
            FROM observations
            WHERE total >= 5
            GROUP BY code, result_band_order
            ORDER BY COUNT(*) DESC, result_band_order DESC
            LIMIT 1
            """)
            .setParameter("studentId", studentId)
            .getResultList();
        return rows.isEmpty() ? null : (String) rows.get(0);
    }
}
