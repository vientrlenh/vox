package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.application.query.repository.SchoolsAtRiskQueryRepository;
import com.sep.vox.domain.common.PageResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolsAtRiskQueryRepository implements SchoolsAtRiskQueryRepository {

    /**
     * Một dòng mỗi TRƯỜNG, kèm sẵn kỳ liên quan và lý do đình chỉ.
     *
     * <p>Gộp trước rồi mới lọc, chứ không lọc thẳng trên bảng kỳ thuê bao: một trường có nhiều kỳ
     * trong lịch sử, nên lọc theo dòng vừa trả trùng trường, vừa xếp một trường đang dùng tốt vào
     * nhóm "hết hạn" chỉ vì kỳ năm ngoái của nó đã kết thúc.
     *
     * <p>{@code COALESCE} bọc từng {@code BOOL_OR} vì hàm này trả NULL khi không dòng nào khớp bộ
     * lọc, và {@code NOT NULL} là NULL chứ không phải TRUE — thiếu nó thì nhóm "lapsed" âm thầm bỏ
     * sót đúng những trường chưa từng có kỳ ACTIVE nào. Cùng cái bẫy đã ghi ở
     * {@code JpaPlatformBusinessHealthQueryRepository}.
     *
     * <p>{@code array_agg(...)[1]} chọn ĐÚNG MỘT kỳ đại diện: kỳ đang phủ sớm hết nhất nếu trường
     * còn gói (cùng kỳ mà {@code MIN(end_date)} dùng để xét "sắp hết hạn", nên tên gói và ngày hết
     * hạn trên màn hình luôn thuộc về cùng một kỳ), còn không thì kỳ kết thúc gần đây nhất.
     */
    private static final String SCHOOL_STATE_CTE = """
        WITH sub AS (
            SELECT
                ss.school_id,
                ss.status,
                ss.end_date,
                ss.suspended_at,
                ss.suspended_reason,
                p.name AS plan_name,
                (ss.status IN ('ACTIVE', 'CANCELLED')
                    AND ss.start_date <= :nowInstant
                    AND ss.end_date >= :nowInstant) AS covering
            FROM school_subscriptions ss
            LEFT JOIN subscription_plans p ON p.id = ss.subscription_plan_id
        ),
        school_state AS (
            SELECT
                school_id,
                COALESCE(BOOL_OR(covering), FALSE) AS covered,
                COALESCE(BOOL_OR(status = 'SUSPENDED'), FALSE) AS suspended,
                MIN(end_date) FILTER (WHERE covering) AS covering_end_date,
                MAX(end_date) AS last_end_date,
                (array_agg(plan_name ORDER BY end_date ASC) FILTER (WHERE covering))[1] AS covering_plan_name,
                (array_agg(plan_name ORDER BY end_date DESC))[1] AS last_plan_name,
                (array_agg(suspended_reason ORDER BY suspended_at DESC NULLS LAST)
                    FILTER (WHERE status = 'SUSPENDED'))[1] AS suspended_reason
            FROM sub
            GROUP BY school_id
        )
        """;

    /**
     * Đi từ {@code schools} rồi LEFT JOIN cả hai phía, để một câu duy nhất phục vụ được cả bốn nhóm.
     *
     * <p>NULL tự loại trường đúng như phép đếm: trường chưa từng mua gói không có dòng trong
     * {@code school_state}, nên {@code NOT st.covered} ra NULL và nó rơi khỏi ba nhóm đầu — giống
     * hệt việc nó không tồn tại trong CTE của câu đếm. Trường chưa từng nạp ví không có dòng số dư,
     * nên {@code bal.balance_vnd < 0} cũng ra NULL và nó rơi khỏi nhóm nợ.
     */
    private static final String FROM_CLAUSE = """
        FROM schools sc
        LEFT JOIN school_state st ON st.school_id = sc.id
        LEFT JOIN school_balances bal ON bal.school_id = sc.id
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<SchoolAtRiskDto> findByBucket(
            SchoolRiskBucket bucket, Instant now, Instant expiringThrough, String keyword, int page, int size) {
        // Xuống dòng ở cuối là BẮT BUỘC: mệnh đề này có thể kết thúc bằng một tham số có tên
        // (:expiringThrough), và nối thẳng với "ORDER BY" ngay sau đó sẽ tạo ra tên tham số
        // ":expiringThroughORDER" -- Hibernate báo "no parameter named" chứ không báo lỗi cú pháp SQL.
        var where = " WHERE " + bucketPredicate(bucket) + keywordPredicate(keyword) + "\n";

        var total = toLong(bind(
            em.createNativeQuery(SCHOOL_STATE_CTE + "SELECT COUNT(*)" + FROM_CLAUSE + where),
            bucket, now, expiringThrough, keyword).getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<Object[]> rows = bind(em.createNativeQuery(SCHOOL_STATE_CTE + """
            SELECT
                sc.id,
                sc.name,
                sc.code,
                COALESCE(st.covering_plan_name, st.last_plan_name),
                COALESCE(st.covering_end_date, st.last_end_date),
                st.suspended_reason,
                COALESCE(bal.balance_vnd, 0)
            """ + FROM_CLAUSE + where + """
            ORDER BY COALESCE(st.covering_end_date, st.last_end_date) ASC NULLS LAST, sc.id ASC
            LIMIT :pageSize OFFSET :offset
            """), bucket, now, expiringThrough, keyword)
            .setParameter("pageSize", size)
            .setParameter("offset", (long) (page - 1) * size)
            .getResultList();

        var content = rows.stream()
            .map(row -> new SchoolAtRiskDto(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                toInstant(row[4]),
                (String) row[5],
                row[6] == null ? BigDecimal.ZERO : new BigDecimal(row[6].toString())))
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    /**
     * Bản sao đúng từng chữ của bốn phép {@code COUNT(*) FILTER} ở
     * {@code JpaPlatformBusinessHealthQueryRepository}. Lệch một chi tiết là thẻ ghi một số còn danh
     * sách sau khi bấm vào lại ra số khác.
     */
    private static String bucketPredicate(SchoolRiskBucket bucket) {
        return switch (bucket) {
            case EXPIRING_SOON -> "st.covered AND st.covering_end_date <= :expiringThrough";
            case LAPSED -> "NOT st.covered AND NOT st.suspended";
            case SUSPENDED -> "NOT st.covered AND st.suspended";
            case IN_DEBT -> "bal.balance_vnd < 0";
        };
    }

    /** Tìm theo tên hoặc mã trường — hai thứ duy nhất người vận hành có trong tay khi nhận một cuộc gọi. */
    private static String keywordPredicate(String keyword) {
        return keyword == null || keyword.isBlank()
            ? ""
            : " AND (LOWER(sc.name) LIKE :keyword OR LOWER(sc.code) LIKE :keyword)";
    }

    private static Query bind(Query query, SchoolRiskBucket bucket, Instant now, Instant expiringThrough,
            String keyword) {
        query.setParameter("nowInstant", now);
        if (bucket == SchoolRiskBucket.EXPIRING_SOON) {
            query.setParameter("expiringThrough", expiringThrough);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }
        return query;
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Instant toInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        throw new IllegalStateException("Không đọc được cột thời gian kiểu " + value.getClass());
    }
}
