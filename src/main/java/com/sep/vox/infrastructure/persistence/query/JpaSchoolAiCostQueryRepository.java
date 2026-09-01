package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.ZoneConstant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolAiCostQueryRepository implements SchoolAiCostQueryRepository {

    /** Cửa sổ NỬA MỞ {@code [from, to)} — cùng quy ước với schoolBalanceSummary và các thẻ dashboard. */
    private static final String WINDOW = """
        WHERE e.school_id = :schoolId
          AND e.occurred_at >= :fromInstant
          AND e.occurred_at < :toInstant
        """;

    /**
     * Ghép bằng {@code formatted} chứ KHÔNG nối hai text block quanh giá trị.
     *
     * <p>Nối text block thì phần thụt đầu dòng của block SAU dính luôn vào giá trị vừa chèn: đơn vị
     * thành {@code 'day    '} và Postgres trả về "unit not recognized", một lỗi chỉ lộ ra lúc chạy
     * chứ không phải lúc biên dịch. Cùng họ với cái bẫy {@code :expiringThroughORDER} đã ghi ở
     * {@code JpaSchoolsAtRiskQueryRepository}.
     */
    private static final String BUCKETED_COST_SQL = """
        SELECT
            date_trunc('%s', e.occurred_at AT TIME ZONE :zone) AT TIME ZONE :zone AS bucket,
            e.quota_type,
            SUM(e.amount_vnd)
        FROM school_ai_spend_entries e
        %s
        GROUP BY bucket, e.quota_type
        ORDER BY bucket ASC
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code date_trunc} chạy trên giờ NGHIỆP VỤ rồi đổi ngược về instant.
     *
     * <p>Bản cũ ({@code JpaTokenUsageTimeseriesQueryRepository}) cắt thẳng trên timestamptz, tức cắt
     * theo UTC — chính javadoc của nó đã ghi nhận điều đó. Hệ quả: mọi khoản phát sinh từ 00:00 tới
     * 07:00 giờ Việt Nam bị xếp vào cột NGÀY HÔM TRƯỚC, và cột cuối biểu đồ luôn thiếu một mẩu.
     *
     * <p>{@code granularityUnit} KHÔNG đi qua tham số bind mà ghép vào chuỗi: Postgres đòi đối số đầu
     * của {@code date_trunc} là hằng, và giá trị chỉ đến từ một enum đóng do chính server dựng — nó
     * không bao giờ là chuỗi người dùng gửi lên.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<AiCostBucketDto> findBucketedCost(
            UUID schoolId, Instant from, Instant to, String granularityUnit) {
        List<Object[]> rows = em.createNativeQuery(BUCKETED_COST_SQL.formatted(granularityUnit, WINDOW))
            .setParameter("zone", ZoneConstant.BUSINESS_ZONE.getId())
            .setParameter("schoolId", schoolId)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getResultList();

        return rows.stream()
            .map(row -> new AiCostBucketDto(
                toInstant(row[0]),
                (String) row[1],
                row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString())))
            .toList();
    }

    /**
     * {@code LEFT JOIN} sang trần chi cá nhân chỉ có thể ra tối đa một dòng mỗi bút toán — khoá duy
     * nhất của bảng đó là (subscription, quota_type, user) — nên phép SUM không bị nhân lên.
     *
     * <p>{@code MAX(allocated_amount_vnd)} chứ không phải một giá trị xác định: cửa sổ có thể vắt qua
     * hai kỳ đăng ký với hai mức trần khác nhau, và không có câu trả lời đúng duy nhất cho "trần chi
     * của người này trong khoảng đó". Lấy mức cao nhất để cột phần trăm không bao giờ vẽ ra một con
     * số vượt 100% chỉ vì trường đã nâng trần giữa chừng.
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageResult<UserAiSpendDto> findSpendByUser(
            UUID schoolId, Instant from, Instant to, String quotaType, int page, int size) {
        var quotaFilter = quotaType == null ? "" : " AND e.quota_type = :quotaType\n";

        // Đếm số NHÓM chứ không số dòng: một người có thể tiêu ở cả hai loại ví và mỗi loại là một
        // hàng riêng trên bảng, nên COUNT(*) trên bút toán sẽ ra tổng số lần tiêu.
        var total = toLong(bind(em.createNativeQuery("""
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM school_ai_spend_entries e
                """ + WINDOW + " AND e.user_id IS NOT NULL\n" + quotaFilter + """
                GROUP BY e.user_id, e.quota_type
            ) grouped
            """), schoolId, from, to, quotaType).getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<Object[]> rows = bind(em.createNativeQuery("""
            SELECT
                e.user_id,
                u.full_name,
                e.quota_type,
                SUM(e.amount_vnd),
                MAX(a.allocated_amount_vnd)
            FROM school_ai_spend_entries e
            JOIN users u ON u.id = e.user_id
            LEFT JOIN school_subscription_quota_user_allocations a
                   ON a.user_id = e.user_id
                  AND a.quota_type = e.quota_type
                  AND a.school_subscription_id = e.subscription_id
            """ + WINDOW + " AND e.user_id IS NOT NULL\n" + quotaFilter + """
            GROUP BY e.user_id, u.full_name, e.quota_type
            ORDER BY SUM(e.amount_vnd) DESC, u.full_name ASC, e.user_id ASC
            LIMIT :pageSize OFFSET :offset
            """), schoolId, from, to, quotaType)
            .setParameter("pageSize", size)
            .setParameter("offset", (long) (page - 1) * size)
            .getResultList();

        var content = rows.stream()
            .map(row -> new UserAiSpendDto(
                toUuid(row[0]),
                (String) row[1],
                (String) row[2],
                new BigDecimal(row[3].toString()),
                row[4] == null ? null : new BigDecimal(row[4].toString())))
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    @Override
    public BigDecimal sumSchoolWideCost(UUID schoolId, Instant from, Instant to, String quotaType) {
        var result = bind(em.createNativeQuery("""
            SELECT COALESCE(SUM(e.amount_vnd), 0)
            FROM school_ai_spend_entries e
            """ + WINDOW + " AND e.user_id IS NULL\n"
            + (quotaType == null ? "" : " AND e.quota_type = :quotaType\n")),
            schoolId, from, to, quotaType)
            .getSingleResult();

        return new BigDecimal(result.toString());
    }

    @Override
    public Instant findFirstRecordedAt(UUID schoolId) {
        return toInstant(em.createNativeQuery("""
            SELECT MIN(e.occurred_at) FROM school_ai_spend_entries e WHERE e.school_id = :schoolId
            """)
            .setParameter("schoolId", schoolId)
            .getSingleResult());
    }

    private static Query bind(Query query, UUID schoolId, Instant from, Instant to, String quotaType) {
        query.setParameter("schoolId", schoolId)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to);
        if (quotaType != null) {
            query.setParameter("quotaType", quotaType);
        }
        return query;
    }
}
