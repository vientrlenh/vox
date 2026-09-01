package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.GradingOutcomeBucketDto;
import com.sep.vox.application.query.dto.LiveSessionCountsDto;
import com.sep.vox.application.query.repository.PlatformOperationalHealthQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaPlatformOperationalHealthQueryRepository implements PlatformOperationalHealthQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Một lượt quét bảng cho cả ba con số nhờ {@code COUNT(...) FILTER (WHERE ...)} của Postgres,
     * thay vì ba câu COUNT riêng. Mệnh đề WHERE ngoài cùng vẫn cần: nó cho Postgres bỏ qua toàn bộ
     * phiên đã kết thúc (GRADED / EXPIRED / GRADING_FAILED), vốn là phần lớn dần theo thời gian.
     */
    @Override
    public LiveSessionCountsDto countLiveSessions() {
        Object[] row = (Object[]) em.createNativeQuery("""
            SELECT
                COUNT(*) FILTER (WHERE status = 'IN_PROGRESS'),
                COUNT(DISTINCT exam_id) FILTER (WHERE status = 'IN_PROGRESS'),
                COUNT(*) FILTER (WHERE status IN ('SUBMITTED', 'GRADING'))
            FROM exam_sessions
            WHERE status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADING')
            """)
            .getSingleResult();

        return new LiveSessionCountsDto(toLong(row[0]), toLong(row[1]), toLong(row[2]));
    }

    /**
     * Gộp ngày ở DB chứ không nạp phiên về đếm trong Java: số phiên thi chỉ tăng theo thời gian, còn
     * kết quả luôn là tối đa vài chục dòng.
     *
     * <p>{@code submitted_at AT TIME ZONE :zone} đổi timestamptz sang giờ địa phương của múi truyền
     * vào rồi mới cắt ngày — không dùng {@code date_trunc} trần như
     * {@code JpaSchoolAiCostQueryRepository}, vì cắt trần là cắt theo UTC.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GradingOutcomeBucketDto> findGradingOutcomeByDay(Instant from, Instant to, ZoneId zone) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                CAST(date_trunc('day', submitted_at AT TIME ZONE CAST(:zone AS varchar)) AS date) AS day,
                COUNT(*) FILTER (WHERE status = 'GRADED'),
                COUNT(*) FILTER (WHERE status = 'GRADING_FAILED')
            FROM exam_sessions
            WHERE status IN ('GRADED', 'GRADING_FAILED')
              AND submitted_at >= :fromInstant
              AND submitted_at < :toInstant
            GROUP BY day
            ORDER BY day ASC
            """)
            .setParameter("zone", zone.getId())
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getResultList();

        return rows.stream()
            .map(row -> new GradingOutcomeBucketDto(toLocalDate(row[0]), toLong(row[1]), toLong(row[2])))
            .toList();
    }

    /**
     * COUNT của Postgres về tới đây có thể là {@code Long} hoặc {@code BigInteger} tùy phiên bản
     * driver/Hibernate, nên ép qua {@link Number} thay vì cast thẳng — cùng loại bẫy đã gây
     * ClassCastException ở {@code JpaSchoolAiCostQueryRepository}.
     */
    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    /** Cột {@code date} về dưới dạng {@link LocalDate} hoặc {@link java.sql.Date} tùy phiên bản. */
    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("Không đọc được cột ngày kiểu " + value.getClass());
    }
}
