package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toInt;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ExamAwaitingPublishDto;
import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.application.query.dto.SchoolUnscoredWorkloadDto;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamSession;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolWorkloadQueryRepository implements SchoolWorkloadQueryRepository {

    /**
     * MỘT định nghĩa của "bài chưa có điểm", dùng chung cho cả hai câu bên dưới.
     *
     * <p>Hai lối vào tập này, và chúng loại trừ nhau theo dựng hình:
     *
     * <ul>
     *   <li>Phiên {@code GRADING_FAILED} CHƯA có dòng kết quả nào — AI hỏng và chưa ai đụng tới.
     *   <li>Dòng kết quả {@code PENDING_REVIEW} — bài đang ở hàng đợi người chấm, bất kể phiên đã
     *       {@code GRADED} hay vẫn mang {@code GRADING_FAILED}. Chuyển sang chấm tay CỐ Ý không đổi
     *       trạng thái phiên (xem {@code HandOffGradingToHumanUseCase}), nên chỉ nhìn trạng thái
     *       phiên sẽ đếm bài đã có người nhận thành bài chưa ai xử lý.
     * </ul>
     *
     * <p>Kỳ đã {@code RESULTS_PUBLISHED} bị LOẠI: cả chấm lại bằng AI lẫn chuyển người chấm đều bị từ
     * chối ở trạng thái đó, nên những bài ấy không còn là việc phải làm mà là thiệt hại đã xảy ra —
     * để chúng trong hàng đợi thì con số có một mức sàn không bao giờ về 0 được. Chúng vẫn hiện ở màn
     * chi tiết phiên, kèm lý do vì sao không còn nút nào.
     *
     * <p>{@code LEFT JOIN} sang phân công chỉ có thể ra tối đa một dòng: {@code active_result_id} là
     * cờ "phân công đang mở", và mỗi bài chỉ có tối đa một vòng đang mở.
     */
    private static final String UNSCORED_CTE = """
        WITH unscored AS (
            SELECT
                s.id           AS session_id,
                s.exam_id      AS exam_id,
                s.submitted_at AS submitted_at,
                CASE
                    WHEN r.id IS NULL AND s.school_regrade_count < :maxSchoolRegrades
                        THEN 'AI_RETRY_LEFT'
                    WHEN r.id IS NULL
                        THEN 'AI_NO_RETRY'
                    WHEN ga.id IS NULL
                        THEN 'AWAITING_ASSIGNMENT'
                    WHEN ga.deadline_at IS NOT NULL AND ga.deadline_at < :now
                        THEN 'ASSIGNED_OVERDUE'
                    ELSE 'ASSIGNED_IN_PROGRESS'
                END AS bucket
            FROM exam_sessions s
            JOIN exams e ON e.id = s.exam_id
            LEFT JOIN exam_candidate_results r
                   ON r.session_id = s.id
                  AND r.status <> 'DELETED'
            LEFT JOIN exam_grading_assignments ga
                   ON ga.active_result_id = r.id
            WHERE e.school_id = :schoolId
              AND e.kind = 'CENTRALIZED'
              AND e.status NOT IN ('CANCELLED', 'RESULTS_PUBLISHED')
              AND s.status <> 'DELETED'
              AND (
                    (s.status = 'GRADING_FAILED' AND r.id IS NULL)
                 OR r.status = 'PENDING_REVIEW'
              )
        )
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code MIN(submitted_at)} trả null khi không còn bài nào — và null ở đây phải đi tới tận giao
     * diện: "hàng đợi rỗng" khác hẳn "có bài, vừa nộp hôm nay", vốn cũng cho ra 0 ngày chờ.
     */
    @Override
    public SchoolUnscoredWorkloadDto countUnscored(UUID schoolId, Instant now) {
        Object[] row = (Object[]) em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                COUNT(*) FILTER (WHERE bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE bucket = 'AI_NO_RETRY'),
                COUNT(*) FILTER (WHERE bucket = 'AWAITING_ASSIGNMENT'),
                COUNT(*) FILTER (WHERE bucket = 'ASSIGNED_OVERDUE'),
                COUNT(*) FILTER (WHERE bucket = 'ASSIGNED_IN_PROGRESS'),
                MIN(submitted_at),
                COUNT(DISTINCT exam_id)
            FROM unscored
            """)
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .setParameter("maxSchoolRegrades", ExamSession.MAX_SCHOOL_REGRADES)
            .getSingleResult();

        return new SchoolUnscoredWorkloadDto(
            toInt(row[0]), toInt(row[1]), toInt(row[2]), toInt(row[3]), toInt(row[4]),
            toInstant(row[5]), toInt(row[6]));
    }

    /**
     * Chỉ kỳ {@code CLOSED}: đó đúng là tập kỳ mà nút "Công bố điểm" đang bấm được. Kỳ còn
     * {@code IN_PROGRESS} vẫn đang có người thi nên bài trống là chuyện bình thường, chưa phải cảnh
     * báo.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<ExamAwaitingPublishDto> findExamsAwaitingPublish(UUID schoolId, Instant now, int limit) {
        List<Object[]> rows = em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                e.id,
                e.code,
                e.name,
                e.close_at,
                COUNT(*),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_NO_RETRY'),
                COUNT(*) FILTER (WHERE u.bucket IN
                    ('AWAITING_ASSIGNMENT', 'ASSIGNED_OVERDUE', 'ASSIGNED_IN_PROGRESS'))
            FROM unscored u
            JOIN exams e ON e.id = u.exam_id
            WHERE e.status = 'CLOSED'
            GROUP BY e.id, e.code, e.name, e.close_at
            ORDER BY COUNT(*) DESC, e.close_at ASC NULLS LAST, e.id ASC
            LIMIT :maxExams
            """)
            .setParameter("schoolId", schoolId)
            .setParameter("now", now)
            .setParameter("maxSchoolRegrades", ExamSession.MAX_SCHOOL_REGRADES)
            .setParameter("maxExams", limit)
            .getResultList();

        return rows.stream()
            .map(row -> new ExamAwaitingPublishDto(
                toUuid(row[0]),
                (String) row[1],
                (String) row[2],
                toInstant(row[3]),
                toInt(row[4]),
                toInt(row[5]),
                toInt(row[6]),
                toInt(row[7])))
            .toList();
    }

    /**
     * Chạy trên CHÍNH {@code unscored} rồi lọc lấy hai nhóm AI, thay vì viết lại mệnh đề "phiên hỏng
     * chưa có kết quả": thẻ trên trang tổng quan và danh sách này phải đếm đúng một tập, và một bản
     * sao của cùng điều kiện chỉ cần lệch một chi tiết là bấm vào con số 14 rồi thấy 12 dòng.
     *
     * <p>Xếp CŨ NHẤT TRƯỚC — ngược với màn của quản trị hệ thống. Bên đó câu hỏi là "vừa hỏng chuyện
     * gì", ở đây là "em nào đã chờ lâu nhất mà vẫn chưa có điểm".
     *
     * <p>{@code now} vẫn phải truyền vào CTE dù hai nhóm AI không dùng tới nó: nó nằm trong nhánh xét
     * quá hạn của các nhóm khác, và Hibernate đòi mọi tham số trong câu đều được gán.
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageResult<SchoolGradingFailureDto> findUnhandledAiFailures(
            UUID schoolId, UUID examId, Boolean retryLeft, int page, int size) {
        // Xuống dòng ở cuối là BẮT BUỘC: mệnh đề có thể kết thúc bằng một tham số có tên (:examId), và
        // nối thẳng với "ORDER BY" ngay sau sẽ tạo ra tên tham số ":examIdORDER" -- Hibernate báo "no
        // parameter named" chứ không báo lỗi cú pháp SQL.
        var where = " WHERE u.bucket IN ('AI_RETRY_LEFT', 'AI_NO_RETRY')"
            + (examId == null ? "" : " AND u.exam_id = :examId")
            + allowancePredicate(retryLeft)
            + "\n";

        var total = toLong(bind(em.createNativeQuery(
            UNSCORED_CTE + "SELECT COUNT(*) FROM unscored u" + where), schoolId, examId)
            .getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<Object[]> rows = bind(em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                u.session_id,
                u.exam_id,
                e.code,
                e.name,
                usr.full_name,
                (SELECT sc.name
                   FROM school_class_users scu
                   JOIN school_classes sc ON sc.id = scu.school_class_id
                  WHERE scu.user_id = c.student_id AND scu.is_active = TRUE
                  ORDER BY sc.name ASC
                  LIMIT 1),
                u.submitted_at,
                s.grading_error,
                s.grading_retry_count,
                u.bucket = 'AI_RETRY_LEFT'
            FROM unscored u
            JOIN exam_sessions s ON s.id = u.session_id
            JOIN exams e ON e.id = u.exam_id
            JOIN exam_candidates c ON c.id = s.candidate_id
            LEFT JOIN users usr ON usr.id = c.student_id
            """ + where + """
            ORDER BY u.submitted_at ASC, u.session_id ASC
            LIMIT :pageSize OFFSET :offset
            """), schoolId, examId)
            .setParameter("pageSize", size)
            .setParameter("offset", (long) (page - 1) * size)
            .getResultList();

        var content = rows.stream()
            .map(row -> new SchoolGradingFailureDto(
                toUuid(row[0]),
                toUuid(row[1]),
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                toInstant(row[6]),
                (String) row[7],
                row[8] == null ? null : ((Number) row[8]).intValue(),
                Boolean.TRUE.equals(row[9])))
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    @Override
    public int[] countAiFailuresByAllowance(UUID schoolId, UUID examId) {
        Object[] row = (Object[]) bind(em.createNativeQuery(UNSCORED_CTE + """
            SELECT
                COUNT(*) FILTER (WHERE u.bucket = 'AI_RETRY_LEFT'),
                COUNT(*) FILTER (WHERE u.bucket = 'AI_NO_RETRY')
            FROM unscored u
            """ + (examId == null ? "" : " WHERE u.exam_id = :examId")), schoolId, examId)
            .getSingleResult();

        return new int[] { toInt(row[0]), toInt(row[1]) };
    }

    /**
     * Ghép thẳng vào chuỗi SQL chứ không truyền tham số Boolean nullable: Postgres không suy được kiểu
     * của một tham số null đứng một mình và sẽ ném "could not determine data type of parameter". Giá
     * trị ghép vào là hằng do chính lớp này viết ra, không phải dữ liệu người dùng.
     */
    private static String allowancePredicate(Boolean retryLeft) {
        if (retryLeft == null) {
            return "";
        }
        return retryLeft ? " AND u.bucket = 'AI_RETRY_LEFT'" : " AND u.bucket = 'AI_NO_RETRY'";
    }

    private Query bind(Query query, UUID schoolId, UUID examId) {
        query.setParameter("schoolId", schoolId)
            .setParameter("now", Instant.now())
            .setParameter("maxSchoolRegrades", ExamSession.MAX_SCHOOL_REGRADES);
        if (examId != null) {
            query.setParameter("examId", examId);
        }
        return query;
    }
}
