package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.metering.AiUsageRecordJpaEntity;

public interface SpringDataAiUsageRecordRepository extends JpaRepository<AiUsageRecordJpaEntity, UUID> {
    List<AiUsageRecordJpaEntity> findByAssessmentSessionId(UUID assessmentSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    // There is NO whole-session sum (the old sumCost*ByAssessmentSessionId): it is the very thing that charged twice
    // when a session was regraded. Every sum used to charge money must go through the chargedAt stamp below.

    /**
     * CLAIMS every uncharged cost row of the session by stamping them with one shared {@code chargedAt}, and returns
     * the number of rows claimed. That stamp is the identity tag of this charge -- see
     * {@link #sumCostVndByAssessmentSessionIdAndChargedAt}.
     *
     * <p>Claim FIRST and sum afterwards, not sum first and then stamp: a usage row inserted by Kafka between the two
     * steps in the opposite order would be stamped "charged" without ever being added to the deduction, i.e.
     * that exact amount would be lost. In this order it stays NULL and the next charge will pick it up.
     *
     * <p>clearAutomatically is not needed: the caller only reads back through an aggregate query and does not reload
     * any entity of this table.
     */
    @Modifying
    @Query("""
        UPDATE AiUsageRecordJpaEntity r
        SET r.chargedAt = :chargedAt
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.chargedAt IS NULL AND r.waivedAt IS NULL
    """)
    int markChargedByAssessmentSessionId(
        @Param("assessmentSessionId") UUID assessmentSessionId, @Param("chargedAt") Instant chargedAt);

    /**
     * WAIVES every undecided cost row of the session: they belong to the grading attempt that just failed, and a failed
     * attempt produces no result for the school, so there is nothing to charge.
     *
     * <p>"Undecided" = not yet charged AND not yet waived. Excluding already-charged rows is mandatory: a session can be
     * graded several times, and earlier attempts were legitimately charged -- waiving over them would refund
     * work that was fully delivered.
     *
     * <p>Called right when the session enters GRADING_FAILED, NOT when someone later presses regrade: waiting until then makes
     * the behavior depend on whether someone goes to fix it -- left alone it is free, fixed it
     * is charged, exactly the wrong incentive.
     */
    @Modifying
    @Query("""
        UPDATE AiUsageRecordJpaEntity r
        SET r.waivedAt = :waivedAt
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.chargedAt IS NULL AND r.waivedAt IS NULL
    """)
    int markWaivedByAssessmentSessionId(
        @Param("assessmentSessionId") UUID assessmentSessionId, @Param("waivedAt") Instant waivedAt);

    /** Total platform cost absorbed for the failed grading attempts of this session. */
    @Query("""
        SELECT COALESCE(SUM(r.costVnd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.waivedAt IS NOT NULL
    """)
    BigDecimal sumWaivedCostVndByAssessmentSessionId(@Param("assessmentSessionId") UUID assessmentSessionId);

    @Query("""
        SELECT COALESCE(SUM(r.costVnd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.chargedAt = :chargedAt
    """)
    BigDecimal sumCostVndByAssessmentSessionIdAndChargedAt(
        @Param("assessmentSessionId") UUID assessmentSessionId, @Param("chargedAt") Instant chargedAt);

    @Query("""
        SELECT COALESCE(SUM(r.costUsd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.chargedAt = :chargedAt
    """)
    BigDecimal sumCostUsdByAssessmentSessionIdAndChargedAt(
        @Param("assessmentSessionId") UUID assessmentSessionId, @Param("chargedAt") Instant chargedAt);
}