package com.sep.vox.infrastructure.persistence.query;

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
        return new LearnerProfileInfo(
            current == null || current.getGoalType() == null
                ? "ABILITY_IMPROVEMENT"
                : current.getGoalType(),
            current == null || current.getFlsaScore() == null
                ? null
                : current.getFlsaScore().doubleValue(),
            (String) target[0],
            (String) target[1],
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
}
