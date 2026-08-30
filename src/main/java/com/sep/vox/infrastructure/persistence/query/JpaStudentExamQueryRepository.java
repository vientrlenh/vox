package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.StudentExamRowInfo;
import com.sep.vox.application.query.repository.StudentExamQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.exam.ExamCandidateStatus;
import com.sep.vox.domain.model.exam.ExamStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

@Repository
public class JpaStudentExamQueryRepository implements StudentExamQueryRepository {

    /**
     * Cao hơn mức 100 của các query repository khác vì client desktop cố ý xin MỘT trang đủ lớn
     * ({@code /api/v1/exams?page=1&size=200}) rồi liệt kê tại chỗ thay vì phân trang trong app.
     * Cắt xuống 100 ở đây là âm thầm giấu bớt bài thi của em học sinh thứ 101.
     */
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * Ca thi học sinh được phép nhìn thấy -- giữ khớp với
     * {@code ExamScheduleStatus#isVisibleToStudent()}. DRAFT là ca chưa publish, MOVED/DELETED đã bị
     * thay thế; CANCELLED vẫn hiện để em biết ca đã huỷ.
     */
    private static final String VISIBLE_SCHEDULE_STATUSES = "'PUBLISHED', 'COMPLETED', 'CANCELLED'";

    /**
     * Bản SQL của {@code StudentExamViewSupport#statusOf}. Phải trùng từng nhánh với bản Java:
     * đây vừa là cột trả về vừa là vị ngữ lọc, nên lệch một nhánh là danh sách và bộ đếm nói khác
     * nhau. Bài kiểm tra trên lớp suy từ trạng thái kỳ thi, kỳ thi tập trung suy từ giờ ca thi.
     */
    private static final String DERIVED_STATUS = """
        CASE
            WHEN e.kind = 'CLASS_TEST' AND e.status = 'IN_PROGRESS' THEN 'in_progress'
            WHEN e.kind = 'CLASS_TEST' AND (e.status = 'DRAFT' OR e.status = 'SCHEDULED') THEN 'upcoming'
            WHEN e.kind = 'CLASS_TEST' THEN 'completed'
            WHEN s.startDate IS NULL OR s.endDate IS NULL THEN 'upcoming'
            WHEN s.startDate <= :now AND s.endDate > :now THEN 'in_progress'
            WHEN s.startDate > :now THEN 'upcoming'
            ELSE 'completed'
        END
        """;

    /**
     * INNER JOIN sang schedule là cố ý: bản Java loại thẳng thí sinh chưa xếp ca
     * ({@code scheduleId == null}) hoặc ca đã biến mất, nên join trong đã đúng ngữ nghĩa đó.
     */
    private static final String FROM_WHERE = """
        FROM ExamCandidateJpaEntity c
        JOIN ExamJpaEntity e ON e.id = c.examId
        JOIN ExamScheduleJpaEntity s ON s.id = c.scheduleId
        WHERE c.studentId = :studentId
          AND e.status <> 'DRAFT'
          AND s.status IN (""" + VISIBLE_SCHEDULE_STATUSES + """
        )
          AND (:examKind IS NULL OR e.kind = :examKind)
          AND (:derivedStatus IS NULL OR (""" + DERIVED_STATUS + """
        ) = :derivedStatus)
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<StudentExamRowInfo> findMyExams(
            UUID studentId,
            String examKind,
            String derivedStatus,
            boolean sortDescending,
            int page,
            int size,
            Instant now) {

        var normalizedPage = Math.max(page, 1);
        var normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        var total = em.createQuery("SELECT COUNT(c.id) " + FROM_WHERE, Long.class)
            .setParameter("studentId", studentId)
            .setParameter("examKind", examKind)
            .setParameter("derivedStatus", derivedStatus)
            .setParameter("now", now)
            .getSingleResult();

        if (total == 0) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0, 0);
        }

        // Bài chưa có ngày thi luôn xếp cuối ở CẢ HAI chiều -- đảo chiều mà kéo chúng lên đầu thì
        // danh sách mở ra toàn dòng trống. `c.id` là khoá phụ để hai bài trùng giờ không đổi chỗ
        // giữa các trang: thiếu nó, phân trang có thể lặp hoặc bỏ sót dòng.
        var direction = sortDescending ? "DESC" : "ASC";
        var rows = em.createQuery("""
            SELECT c.id, c.status, c.blockedAt, c.assignedPaperId,
                   e.id, e.name, e.description, e.kind, e.status, e.requiresOtp, e.maxAttempt,
                   s.id, s.startDate, s.endDate,
                   COALESCE(s.startDate, e.openAt),
                   (""" + DERIVED_STATUS + """
            )
            """ + FROM_WHERE + """
            ORDER BY COALESCE(s.startDate, e.openAt) """ + direction + """
             NULLS LAST, c.id ASC
            """, Tuple.class)
            .setParameter("studentId", studentId)
            .setParameter("examKind", examKind)
            .setParameter("derivedStatus", derivedStatus)
            .setParameter("now", now)
            .setFirstResult((normalizedPage - 1) * normalizedSize)
            .setMaxResults(normalizedSize)
            .getResultList();

        var content = new ArrayList<StudentExamRowInfo>(rows.size());
        for (var row : rows) {
            content.add(new StudentExamRowInfo(
                row.get(0, UUID.class),
                candidateStatusOf(row.get(1, String.class)),
                row.get(2, Instant.class),
                row.get(3, UUID.class),
                row.get(4, UUID.class),
                row.get(5, String.class),
                row.get(6, String.class),
                row.get(7, String.class),
                examStatusOf(row.get(8, String.class)),
                Boolean.TRUE.equals(row.get(9, Boolean.class)),
                row.get(10, Integer.class),
                row.get(11, UUID.class),
                row.get(12, Instant.class),
                row.get(13, Instant.class),
                row.get(14, Instant.class),
                row.get(15, String.class)));
        }

        var totalPages = (int) Math.ceil(total / (double) normalizedSize);
        return new PageResult<>(content, normalizedPage, normalizedSize, total, totalPages);
    }

    /**
     * Cột {@code status} lưu dạng chuỗi, nên một giá trị lạ (dữ liệu cũ, enum vừa đổi tên) không
     * được phép làm hỏng cả trang. Trả null để tầng trên xử theo lối "không xác định" -- các hàm
     * {@code ExamCandidateStatus#isBlockedForEntry}/{@code isAttended} đều đã null-safe.
     */
    private static ExamCandidateStatus candidateStatusOf(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ExamCandidateStatus.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static ExamStatus examStatusOf(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ExamStatus.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
