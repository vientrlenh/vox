package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.MonitoredExamSummary;
import com.sep.vox.application.query.repository.MonitoredExamQueryRepository;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaMonitoredExamQueryRepository implements MonitoredExamQueryRepository {

    /**
     * Ca có thể có gì đó để giám sát.
     *
     * <p>Loại DRAFT (chưa công bố thì chưa ai thi được), CANCELLED, và cả DELETED/MOVED. Giữ
     * COMPLETED vì cửa sổ thời gian bên dưới đã lọc hộ rồi: một ca vừa bị đánh dấu hoàn thành trong
     * lúc học viên còn đang phát thì vẫn phải mở xem được, và ở màn giám sát thì bỏ sót một phòng
     * đang chạy tệ hơn nhiều so với hiện thừa một dòng.
     */
    private static final Set<String> MONITORABLE_STATUSES = Set.of(
        ExamScheduleStatus.PUBLISHED.name(),
        ExamScheduleStatus.COMPLETED.name());

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<MonitoredExamSummary> findMonitorableByTeacher(
            UUID teacherId, UUID examId, Instant now, Instant leadUntil) {
        return em.createQuery(select("""
                JOIN ExamScheduleProctorJpaEntity p
                    ON p.scheduleId = sch.id AND p.teacherId = :teacherId
            """, ""), MonitoredExamSummary.class)
            .setParameter("teacherId", teacherId)
            .setParameter("examId", examId)
            .setParameter("now", now)
            .setParameter("leadUntil", leadUntil)
            .setParameter("statuses", MONITORABLE_STATUSES)
            .getResultList();
    }

    @Override
    public List<MonitoredExamSummary> findMonitorableBySchool(
            UUID schoolId, UUID examId, Instant now, Instant leadUntil) {
        return em.createQuery(select("", " AND exam.schoolId = :schoolId\n"), MonitoredExamSummary.class)
            .setParameter("schoolId", schoolId)
            .setParameter("examId", examId)
            .setParameter("now", now)
            .setParameter("leadUntil", leadUntil)
            .setParameter("statuses", MONITORABLE_STATUSES)
            .getResultList();
    }

    /**
     * Thân chung của hai đường đọc; chỉ khác nhau ở cách chứng minh quyền.
     *
     * <p>Gộp lại vì phần còn lại -- cửa sổ thời gian, phép gom theo kỳ thi, cách đếm ca đang chạy --
     * phải giống hệt nhau: giám thị và school admin nhìn cùng một phòng thi thì không được thấy hai
     * con số khác nhau.
     */
    private String select(String accessJoin, String accessWhere) {
        return """
            SELECT NEW com.sep.vox.application.query.dto.MonitoredExamSummary(
                exam.id,
                exam.code,
                exam.name,
                exam.kind,
                exam.status,
                MIN(sch.startDate),
                MAX(sch.endDate),
                SUM(CASE WHEN sch.startDate <= :now AND sch.endDate > :now THEN 1L ELSE 0L END)
            )
            FROM ExamScheduleJpaEntity sch
            JOIN ExamJpaEntity exam ON exam.id = sch.examId
            """
            + accessJoin
            + """
            WHERE sch.status IN :statuses
              AND (:examId IS NULL OR exam.id = :examId)
              AND (:leadUntil IS NULL OR (sch.startDate <= :leadUntil AND sch.endDate > :now))
            """
            + accessWhere
            + """
            GROUP BY exam.id, exam.code, exam.name, exam.kind, exam.status
            ORDER BY MIN(sch.startDate)
            """;
    }
}
