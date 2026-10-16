package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.projection.dto.MonitoredAssessmentSummary;
import com.sep.vox.application.projection.repository.MonitoredAssessmentQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class JpaMonitoredAssessmentQueryRepository implements MonitoredAssessmentQueryRepository {

    /**
     * Ca có thể có gì đó để giám sát.
     *
     * <p>Loại DRAFT (chưa công bố thì chưa ai thi được), CANCELLED, và cả DELETED/MOVED. Giữ
     * COMPLETED vì cửa sổ thời gian bên dưới đã lọc hộ rồi: một ca vừa bị đánh dấu hoàn thành trong
     * lúc học viên còn đang phát thì vẫn phải mở xem được, và ở màn giám sát thì bỏ sót một phòng
     * đang chạy tệ hơn nhiều so với hiện thừa một dòng.
     */
    private static final Set<String> MONITORABLE_STATUSES = Set.of(
        AssessmentScheduleStatus.PUBLISHED.name(),
        AssessmentScheduleStatus.COMPLETED.name());

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<MonitoredAssessmentSummary> findMonitorableByTeacher(
            UUID teacherId, UUID assessmentId, Instant now, Instant leadUntil) {
        return query("""
                JOIN AssessmentScheduleProctorJpaEntity p
                    ON p.scheduleId = sch.id AND p.teacherId = :teacherId
            """, "", assessmentId, now, leadUntil)
            .setParameter("teacherId", teacherId)
            .getResultList();
    }

    @Override
    public List<MonitoredAssessmentSummary> findMonitorableBySchool(
            UUID schoolId, UUID assessmentId, Instant now, Instant leadUntil) {
        return query("", " AND assessment.schoolId = :schoolId\n", assessmentId, now, leadUntil)
            .setParameter("schoolId", schoolId)
            .getResultList();
    }

    /** Dựng câu và gán phần tham số dùng chung; hai đường đọc chỉ còn phải gán bằng chứng quyền. */
    private TypedQuery<MonitoredAssessmentSummary> query(
            String accessJoin, String accessWhere, UUID assessmentId, Instant now, Instant leadUntil) {
        var query = em
            .createQuery(select(accessJoin, accessWhere, leadUntil != null), MonitoredAssessmentSummary.class)
            .setParameter("assessmentId", assessmentId)
            .setParameter("now", now)
            .setParameter("statuses", MONITORABLE_STATUSES);
        if (leadUntil != null) {
            query.setParameter("leadUntil", leadUntil);
        }
        return query;
    }

    /**
     * Thân chung của hai đường đọc; chỉ khác nhau ở cách chứng minh quyền.
     *
     * <p>Gộp lại vì phần còn lại -- cửa sổ thời gian, phép gom theo kỳ thi, cách đếm ca đang chạy --
     * phải giống hệt nhau: giám thị và school admin nhìn cùng một phòng thi thì không được thấy hai
     * con số khác nhau.
     *
     * <p>Lọc trạng thái CA thôi không đủ, nên có thêm {@code assessment.status <> 'DRAFT'}: hai cột trạng
     * thái là hai máy độc lập, và luồng chuẩn bắt publish từng ca TRƯỚC rồi mới đẩy kỳ thi
     * {@code DRAFT -> SCHEDULED}, nên ca PUBLISHED nằm dưới kỳ thi còn DRAFT là dữ liệu hợp lệ --
     * thiếu nó là cả hai vai đều thấy kỳ thi nhà trường chưa công bố. Chỉ ẩn DRAFT: kỳ thi CANCELLED
     * vẫn hiện kèm trạng thái, cùng luật với {@code JpaProctorScheduleQueryRepository}.
     *
     * @param windowed cửa sổ thời gian được GHÉP VÀO hay bỏ hẳn khỏi câu -- cố ý không viết
     *                 {@code :leadUntil IS NULL OR ...} như chỗ lọc theo assessmentId ngay bên dưới.
     *                 Bản đầu viết vậy và nổ trên Postgres: {@code could not determine data type of
     *                 parameter $8}. Driver pgjdbc gửi tham số thời gian KHÔNG kèm OID kiểu để server
     *                 tự chọn giữa timestamp và timestamptz, nên một {@code $8 IS NULL} đứng trơ
     *                 không còn gì để suy ra kiểu. {@code :assessmentId IS NULL} thì sống vì UUID có OID
     *                 riêng -- cùng một lối viết, chỉ khác kiểu Java, nên đây là cái bẫy im lặng:
     *                 nó qua được cả compile lẫn lúc Hibernate dịch JPQL sang SQL.
     */
    private String select(String accessJoin, String accessWhere, boolean windowed) {
        return """
            SELECT NEW com.sep.vox.application.query.dto.MonitoredAssessmentSummary(
                assessment.id,
                assessment.code,
                assessment.name,
                assessment.kind,
                assessment.status,
                MIN(sch.startDate),
                MAX(sch.endDate),
                SUM(CASE WHEN sch.startDate <= :now AND sch.endDate > :now THEN 1L ELSE 0L END)
            )
            FROM AssessmentScheduleJpaEntity sch
            JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
            """
            + accessJoin
            + """
            WHERE sch.status IN :statuses
              AND assessment.status <> 'DRAFT'
              AND (:assessmentId IS NULL OR assessment.id = :assessmentId)
            """
            + (windowed ? "  AND sch.startDate <= :leadUntil AND sch.endDate > :now\n" : "")
            + accessWhere
            + """
            GROUP BY assessment.id, assessment.code, assessment.name, assessment.kind, assessment.status
            ORDER BY MIN(sch.startDate)
            """;
    }
}
