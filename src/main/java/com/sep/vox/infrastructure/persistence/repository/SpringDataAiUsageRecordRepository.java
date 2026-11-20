package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.AiUsageRecordJpaEntity;

public interface SpringDataAiUsageRecordRepository extends JpaRepository<AiUsageRecordJpaEntity, UUID> {
    List<AiUsageRecordJpaEntity> findByExamSessionId(UUID examSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    // KHÔNG có bản cộng cả phiên (sumCost*ByExamSessionId cũ): nó là chính thứ đã thu tiền hai lần
    // khi một phiên được chấm lại. Mọi lần cộng để thu tiền phải đi qua mốc chargedAt bên dưới.

    /**
     * GIÀNH mọi dòng chi phí chưa thu của phiên bằng cách đóng cùng một mốc {@code chargedAt}, và trả
     * về số dòng đã giành. Mốc đó chính là thẻ định danh của lượt thu này -- xem
     * {@link #sumCostVndByExamSessionIdAndChargedAt}.
     *
     * <p>Giành TRƯỚC rồi mới cộng, chứ không cộng trước rồi đóng dấu: một dòng usage do Kafka chèn vào
     * giữa hai bước ở chiều ngược lại sẽ bị đóng dấu "đã thu" mà chưa hề được cộng vào khoản trừ, tức
     * mất trắng đúng khoản tiền đó. Theo chiều này thì nó vẫn còn NULL và lượt thu sau sẽ nhặt được.
     *
     * <p>clearAutomatically không cần: chỗ gọi chỉ đọc lại bằng truy vấn tổng hợp, không nạp lại
     * entity nào của bảng này.
     */
    @Modifying
    @Query("""
        UPDATE AiUsageRecordJpaEntity r
        SET r.chargedAt = :chargedAt
        WHERE r.examSessionId = :examSessionId AND r.chargedAt IS NULL AND r.waivedAt IS NULL
    """)
    int markChargedByExamSessionId(
        @Param("examSessionId") UUID examSessionId, @Param("chargedAt") Instant chargedAt);

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
        WHERE r.examSessionId = :examSessionId AND r.chargedAt IS NULL AND r.waivedAt IS NULL
    """)
    int markWaivedByExamSessionId(
        @Param("examSessionId") UUID examSessionId, @Param("waivedAt") Instant waivedAt);

    /** Tổng chi phí nền tảng đã gánh thay cho các lượt chấm hỏng của phiên này. */
    @Query("""
        SELECT COALESCE(SUM(r.costVnd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.examSessionId = :examSessionId AND r.waivedAt IS NOT NULL
    """)
    BigDecimal sumWaivedCostVndByExamSessionId(@Param("examSessionId") UUID examSessionId);

    @Query("""
        SELECT COALESCE(SUM(r.costVnd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.examSessionId = :examSessionId AND r.chargedAt = :chargedAt
    """)
    BigDecimal sumCostVndByExamSessionIdAndChargedAt(
        @Param("examSessionId") UUID examSessionId, @Param("chargedAt") Instant chargedAt);

    @Query("""
        SELECT COALESCE(SUM(r.costUsd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.examSessionId = :examSessionId AND r.chargedAt = :chargedAt
    """)
    BigDecimal sumCostUsdByExamSessionIdAndChargedAt(
        @Param("examSessionId") UUID examSessionId, @Param("chargedAt") Instant chargedAt);
}