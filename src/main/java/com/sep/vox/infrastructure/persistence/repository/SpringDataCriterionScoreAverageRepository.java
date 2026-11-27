package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeSessionJpaEntity;

/**
 * Read-only repository aggregating assessment scores + practice scores per criterion -- there is no owning entity, so it uses the
 * {@link Repository} marker instead of {@code JpaRepository} because standard CRUD is not needed.
 */
public interface SpringDataCriterionScoreAverageRepository
        extends Repository<PracticeSessionJpaEntity, UUID> {

    /**
     * The {@code UNION ALL} below is carried over verbatim from
     * {@code SpringDataWeaknessScoreObservationViewRepository.findAllValidScoreObservations}
     * (deleted), keeping the validity conditions that were considered earlier:
     *
     * <ul>
     *   <li>{@code marked_invalid = false} -- a result marked invalid is not counted</li>
     *   <li>{@code blocked_at IS NULL} -- if the candidate is blocked the result is not used</li>
     *   <li>{@code EXISTS (... turns)} -- an evaluation not tied to any speaking turn is an empty record,
     *       counting it would pull the average down with a number that represents nobody</li>
     * </ul>
     *
     * <p>One decisive difference: the old query did NOT filter by student (it scanned the whole system and only then grouped
     * in memory, to serve centering by class). Here {@code :studentId} is in both
     * sides of the union, so the number of rows read is proportional to one student, not to the whole school.
     *
     * <p>Normalized to 0..1 by the criterion's own range ({@code min_score}/{@code max_score} on
     * {@code rubric_criterions}) before averaging: without normalization a 0-10 scale criterion
     * always looks "better" than a 0-5 scale criterion, and the weakest-first order would be wrong. The columns are all
     * {@code numeric}, so this is real division, not integer division.
     *
     * <p>{@code ORDER BY} also has {@code criterion_code} as a tiebreaker so that two criteria with equal scores
     * still get a stable order between calls -- the slot rotation cycle depends on this order; if it changed
     * randomly the same student in the same slot would get a different criterion.
     */
    @Query(value = """
        WITH scores AS (
            SELECT fc.code AS criterion_code,
                   (cs.final_score - rc.min_score)
                       / NULLIF(rc.max_score - rc.min_score, 0) AS normalized
            FROM assessment_item_criterion_scores cs
            JOIN rubric_criterions rc ON rc.id = cs.rubric_criterion_id
            JOIN framework_criteria fc ON fc.id = rc.framework_criterion_id
            JOIN assessment_item_evaluations e ON e.id = cs.evaluation_id
            JOIN assessment_item_responses r ON r.id = e.response_id
            JOIN assessment_sessions s ON s.id = r.session_id
            JOIN assessment_candidates ec ON ec.id = s.candidate_id
            WHERE ec.student_id = :studentId
              AND e.marked_invalid = false
              AND ec.blocked_at IS NULL
              -- Native query, so it must NOT filter by entity: soft-deleted sessions must be excluded by hand, otherwise
              -- the score of a deleted result still pulls down the student's average ability.
              AND s.status <> 'DELETED'
              AND cs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM assessment_item_evaluation_turns t
                  WHERE t.evaluation_id = e.id
              )
            UNION ALL
            -- Practice score: fixed 0-100 scale, identified by CODE, no rubric join.
            --
            -- Divide by 100, not by the rubric range: both sources must be reduced to 0..1 for the
            -- comparison to mean anything. Using the other side's range by mistake is a SILENTLY WRONG run -- no failure,
            -- practice scores just overwhelm assessment scores and Q1 picks the wrong criterion forever.
            SELECT UPPER(TRIM(pcs.criterion_code)) AS criterion_code,
                   pcs.final_score / 100.0 AS normalized
            FROM practice_criterion_scores pcs
            JOIN practice_item_evaluations pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_responses pr ON pr.id = pe.practice_response_id
            JOIN practice_sessions ps ON ps.id = pr.practice_session_id
            WHERE ps.student_id = :studentId
              AND pe.marked_invalid = false
              AND pcs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turns pt
                  WHERE pt.practice_response_id = pr.id
              )
        )
        -- UPPER here because the three sources above may differ in letter case: framework_criteria.code
        -- and practice_criterion_scores.criterion_code do not share a convention.
        SELECT UPPER(criterion_code) AS criterion_code
        FROM scores
        WHERE normalized IS NOT NULL
        GROUP BY UPPER(criterion_code)
        ORDER BY AVG(normalized), UPPER(criterion_code)
        """, nativeQuery = true)
    List<String> findCriterionCodesOrderedByLowestAverageScore(@Param("studentId") UUID studentId);
}
