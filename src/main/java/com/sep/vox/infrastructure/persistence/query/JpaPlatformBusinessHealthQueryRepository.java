package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaPlatformBusinessHealthQueryRepository implements PlatformBusinessHealthQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Gộp về MỘT DÒNG MỖI TRƯỜNG trước rồi mới đếm, chứ không đếm thẳng trên bảng kỳ thuê bao: một
     * trường có nhiều kỳ trong lịch sử, nên đếm theo dòng vừa cộng trùng, vừa xếp trường đang dùng
     * tốt vào nhóm "hết hạn" chỉ vì kỳ năm ngoái đã hết.
     *
     * <p>{@code COALESCE} bọc từng {@code BOOL_OR}: hàm này trả NULL khi không có dòng nào khớp bộ
     * lọc, và {@code NOT NULL} là NULL chứ không phải TRUE — thiếu nó thì nhóm "lapsed" âm thầm bỏ
     * sót đúng những trường chưa từng có kỳ ACTIVE nào.
     */
    @Override
    public SchoolSubscriptionHealthDto countSchoolSubscriptionHealth(Instant now, Instant expiringThrough) {
        Object[] row = (Object[]) em.createNativeQuery("""
            WITH school_state AS (
                SELECT
                    school_id,
                    COALESCE(BOOL_OR(
                        status IN ('ACTIVE', 'CANCELLED')
                        AND start_date <= :nowInstant
                        AND end_date >= :nowInstant
                    ), FALSE) AS covered,
                    COALESCE(BOOL_OR(status = 'SUSPENDED'), FALSE) AS suspended,
                    MIN(end_date) FILTER (WHERE
                        status IN ('ACTIVE', 'CANCELLED')
                        AND start_date <= :nowInstant
                        AND end_date >= :nowInstant
                    ) AS covering_end_date
                FROM school_subscriptions
                GROUP BY school_id
            )
            SELECT
                COUNT(*) FILTER (WHERE covered),
                COUNT(*) FILTER (WHERE covered AND covering_end_date <= :expiringThrough),
                COUNT(*) FILTER (WHERE NOT covered AND NOT suspended),
                COUNT(*) FILTER (WHERE NOT covered AND suspended)
            FROM school_state
            """)
            .setParameter("nowInstant", now)
            .setParameter("expiringThrough", expiringThrough)
            .getSingleResult();

        return new SchoolSubscriptionHealthDto(toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
    }

    @Override
    public long countSchoolsInDebt() {
        return toLong(em.createNativeQuery("""
            SELECT COUNT(*) FROM school_balances WHERE balance_vnd < 0
            """)
            .getSingleResult());
    }

    @Override
    public BigDecimal sumAiCostVnd(Instant from, Instant to) {
        var total = em.createNativeQuery("""
            SELECT COALESCE(SUM(cost_vnd), 0)
            FROM ai_usage_records
            WHERE occurred_at >= :fromInstant
              AND occurred_at < :toInstant
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getSingleResult();

        return total == null ? BigDecimal.ZERO : new BigDecimal(total.toString());
    }

    /** COUNT về đây có thể là {@code Long} hoặc {@code BigInteger} tùy phiên bản driver/Hibernate. */
    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
