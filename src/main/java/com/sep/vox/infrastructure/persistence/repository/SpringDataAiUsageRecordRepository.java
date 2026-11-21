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
     * MIỄN mọi dòng chi phí chưa ngã ngũ của phiên: chúng thuộc lượt chấm vừa hỏng, và lượt hỏng
     * không tạo ra kết quả nào cho trường nên không có gì để thu.
     *
     * <p>"Chưa ngã ngũ" = chưa thu VÀ chưa miễn. Loại dòng đã thu ra là bắt buộc: một phiên có thể
     * chấm nhiều lượt, và những lượt trước đã thu tiền hợp lệ -- miễn đè lên chúng là hoàn tiền cho
     * phần việc đã giao đủ.
     *
     * <p>Gọi ngay lúc phiên vào GRADING_FAILED, KHÔNG đợi tới lúc ai đó bấm chấm lại: để tới đó thì
     * hành vi phụ thuộc vào việc có người đi khắc phục hay không -- bỏ mặc thì miễn phí, còn đi sửa
     * thì bị tính tiền, đúng chiều khuyến khích ngược.
     */
    @Modifying
    @Query("""
        UPDATE AiUsageRecordJpaEntity r
        SET r.waivedAt = :waivedAt
        WHERE r.assessmentSessionId = :assessmentSessionId AND r.chargedAt IS NULL AND r.waivedAt IS NULL
    """)
    int markWaivedByAssessmentSessionId(
        @Param("assessmentSessionId") UUID assessmentSessionId, @Param("waivedAt") Instant waivedAt);

    /** Tổng chi phí nền tảng đã gánh thay cho các lượt chấm hỏng của phiên này. */
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