package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionScoreWithCodeInfo;
import com.sep.vox.infrastructure.persistence.entity.PracticeCriterionScoreJpaEntity;

public interface SpringDataPracticeCriterionScoreRepository
        extends JpaRepository<PracticeCriterionScoreJpaEntity, UUID> {

    List<PracticeCriterionScoreJpaEntity> findByPracticeEvaluationId(UUID practiceEvaluationId);

    Optional<PracticeCriterionScoreJpaEntity> findByPracticeEvaluationIdAndRubricCriterionId(
        UUID practiceEvaluationId,
        UUID rubricCriterionId
    );

    @Query(value = """
        SELECT criterion.code AS code, score.final_score AS finalScore, score.matched_band_code AS matchedBandCode
        FROM practice_criterion_score score
        JOIN rubric_criterions rubric
          ON rubric.id = score.rubric_criterion_id
        JOIN framework_criteria criterion
          ON criterion.id = rubric.framework_criterion_id
        JOIN practice_item_evaluation evaluation
          ON evaluation.id = score.practice_evaluation_id
        JOIN practice_item_response response
          ON response.id = evaluation.practice_response_id
        WHERE response.practice_session_id = :sessionId
        ORDER BY criterion.criteria_order
        """, nativeQuery = true)
    List<CriterionScoreWithCodeInfo> findScoresBySessionId(@Param("sessionId") UUID sessionId);
}
