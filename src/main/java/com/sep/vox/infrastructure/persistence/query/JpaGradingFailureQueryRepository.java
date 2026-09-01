package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.GradingFailureGroupDto;
import com.sep.vox.application.query.dto.GradingFailureSessionDto;
import com.sep.vox.application.query.dto.GradingFailureTotalsDto;
import com.sep.vox.application.query.repository.GradingFailureQueryRepository;
import com.sep.vox.domain.common.PageResult;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaGradingFailureQueryRepository implements GradingFailureQueryRepository {

    /**
     * Tập phiên lỗi trong cửa sổ, kèm vừa đủ cột để cả ba câu bên dưới dùng chung MỘT định nghĩa.
     *
     * <p>Viết một lần rồi nối vào từng câu, thay vì chép lại mệnh đề WHERE ba lần: thẻ tóm tắt, thẻ
     * nhóm và danh sách phiên phải đếm đúng cùng một tập, và ba bản sao của cùng một điều kiện chỉ
     * cần lệch một chi tiết là ba con số trên cùng màn hình nói ba chuyện khác nhau.
     */
    private static final String FAILED_SESSIONS_CTE = """
        WITH failed AS (
            SELECT
                s.id                                            AS session_id,
                s.submitted_at                                  AS failed_at,
                s.grading_error                                 AS error,
                s.grading_retry_count                           AS retry_count,
                vox_grading_error_signature(s.grading_error)    AS signature,
                s.exam_id                                       AS exam_id,
                e.school_id                                     AS school_id,
                e.status <> 'RESULTS_PUBLISHED'                 AS retryable
            FROM exam_sessions s
            JOIN exams e ON e.id = s.exam_id
            WHERE s.status = 'GRADING_FAILED'
              AND s.submitted_at >= :fromInstant
              AND s.submitted_at < :toInstant
        )
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code COUNT(DISTINCT signature)} BỎ QUA NULL, nên nhóm "không rõ nguyên nhân" phải cộng tay —
     * thiếu nó thì một hệ thống mà mọi phiên đều hỏng qua đường DLT sẽ báo "0 nguyên nhân" trong khi
     * màn hình bên dưới đang hiện một nhóm.
     */
    @Override
    public GradingFailureTotalsDto countTotals(Instant from, Instant to) {
        Object[] row = (Object[]) em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT
                COUNT(*),
                COUNT(DISTINCT signature)
                    + CASE WHEN COUNT(*) FILTER (WHERE signature IS NULL) > 0 THEN 1 ELSE 0 END,
                COUNT(DISTINCT school_id),
                COUNT(*) FILTER (WHERE retryable)
            FROM failed
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getSingleResult();

        return new GradingFailureTotalsDto(toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
    }

    /**
     * {@code array_agg(...)[1]} lấy thông điệp thô của phiên hỏng GẦN NHẤT làm mẫu đại diện. Nhóm
     * đứng tên bằng chữ ký đã chuẩn hóa (đã thay uuid và số bằng chỗ trống), mà thứ đó không đọc
     * được — người trực cần thấy một câu lỗi thật.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<GradingFailureGroupDto> findGroups(Instant from, Instant to, int limit) {
        List<Object[]> rows = em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT
                signature,
                (array_agg(error ORDER BY failed_at DESC))[1],
                COUNT(*),
                COUNT(DISTINCT school_id),
                COUNT(DISTINCT exam_id),
                MIN(failed_at),
                MAX(failed_at),
                COUNT(*) FILTER (WHERE retryable)
            FROM failed
            GROUP BY signature
            ORDER BY COUNT(*) DESC, MAX(failed_at) DESC
            LIMIT :maxGroups
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("maxGroups", limit)
            .getResultList();

        return rows.stream()
            .map(row -> new GradingFailureGroupDto(
                (String) row[0],
                (String) row[1],
                toLong(row[2]),
                toLong(row[3]),
                toLong(row[4]),
                toInstant(row[5]),
                toInstant(row[6]),
                toLong(row[7])))
            .toList();
    }

    /**
     * {@code IS NOT DISTINCT FROM} chứ không phải {@code =}: nhóm "không rõ nguyên nhân" có chữ ký
     * null, và {@code null = null} trong SQL ra null nên bấm vào nhóm đó sẽ trả về danh sách rỗng
     * trong khi thẻ nhóm vẫn hiện số phiên.
     *
     * <p>Sắp xếp kèm {@code session_id} làm khóa phụ: nhiều phiên của cùng một ca thi hỏng trong
     * cùng một giây, và chỉ xếp theo thời gian thì thứ tự giữa chúng không xác định — phân trang sẽ
     * lặp phiên ở trang này và đánh rơi phiên ở trang kia.
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageResult<GradingFailureSessionDto> findSessions(
            Instant from, Instant to, String signature, int page, int size) {
        var total = toLong(em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT COUNT(*) FROM failed WHERE signature IS NOT DISTINCT FROM :signature
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("signature", signature)
            .getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<Object[]> rows = em.createNativeQuery(FAILED_SESSIONS_CTE + """
            SELECT
                f.session_id,
                f.school_id,
                sc.name,
                sc.code,
                f.exam_id,
                e.name,
                u.full_name,
                f.failed_at,
                f.retry_count,
                f.error,
                f.retryable,
                EXISTS (SELECT 1 FROM exam_candidate_results r WHERE r.session_id = f.session_id)
            FROM failed f
            JOIN exams e ON e.id = f.exam_id
            JOIN exam_sessions s ON s.id = f.session_id
            JOIN exam_candidates c ON c.id = s.candidate_id
            LEFT JOIN schools sc ON sc.id = f.school_id
            LEFT JOIN users u ON u.id = c.student_id
            WHERE f.signature IS NOT DISTINCT FROM :signature
            ORDER BY f.failed_at DESC, f.session_id ASC
            LIMIT :pageSize OFFSET :offset
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .setParameter("signature", signature)
            .setParameter("pageSize", size)
            .setParameter("offset", (long) (page - 1) * size)
            .getResultList();

        var content = rows.stream()
            .map(row -> new GradingFailureSessionDto(
                toUuid(row[0]),
                toUuid(row[1]),
                (String) row[2],
                (String) row[3],
                toUuid(row[4]),
                (String) row[5],
                (String) row[6],
                toInstant(row[7]),
                row[8] == null ? null : ((Number) row[8]).intValue(),
                (String) row[9],
                Boolean.TRUE.equals(row[10]),
                Boolean.TRUE.equals(row[11])))
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

}
