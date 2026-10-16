package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.projection.dto.ProctorScheduleSummary;
import com.sep.vox.application.projection.repository.ProctorScheduleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Danh sách ca thi của màn điểm danh, cho giám thị và cho quản trị nhà trường.
 *
 * <p>Hai câu dưới đây lọc theo HAI cột trạng thái, không phải một. {@code assessments.status} và
 * {@code assessment_schedules.status} là hai máy trạng thái độc lập, và luồng chuẩn bắt người xếp lịch
 * publish từng ca TRƯỚC rồi mới đẩy kỳ thi {@code DRAFT -> SCHEDULED} -- {@code UpdateAssessmentStatusUseCase}
 * từ chối lên lịch khi còn ca DRAFT. Nên "ca PUBLISHED nằm dưới kỳ thi DRAFT" là trạng thái mọi kỳ
 * thi đều đi qua, không phải dữ liệu hỏng: bỏ {@code assessment.status <> 'DRAFT'} là màn điểm danh hiện
 * kỳ thi nhà trường chưa công bố, đúng lỗi đã xảy ra một lần.
 *
 * <p>Chỉ ẩn DRAFT, cùng luật với {@code JpaStudentAssessmentQueryRepository} và
 * {@code ViewMyAssessmentSchedulesUseCase}: kỳ thi CANCELLED vẫn hiện để người dùng biết kỳ thi đã bị huỷ.
 */
@Repository
public class JpaProctorScheduleQueryRepository implements ProctorScheduleQueryRepository {

    /**
     * Ca không được hiện trong màn điểm danh.
     *
     * <p>DELETED/MOVED thì đã rõ: xoá mềm hoặc đã dời sang ca khác nên không còn là ca thật. DRAFT là
     * ca CHƯA công bố -- publish mới là lúc hệ thống bắt buộc đủ giám thị và mọi thí sinh đã có đề
     * (xem {@code UpdateAssessmentScheduleStatusUseCase#publish}), nên trước đó ca vẫn đang xếp dở, chưa có
     * gì để điểm danh. Cùng luật với {@code JpaMonitoredAssessmentQueryRepository}, hai câu đọc của cùng
     * một màn không được đếm khác nhau.
     *
     * <p>CANCELLED cố ý vẫn hiện, kèm trạng thái, để giám thị biết ca đã huỷ mà không tới phòng.
     */
    private static final Set<String> HIDDEN_SCHEDULE_STATUSES = Set.of(
        AssessmentScheduleStatus.DRAFT.name(),
        AssessmentScheduleStatus.DELETED.name(),
        AssessmentScheduleStatus.MOVED.name());

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProctorScheduleSummary> findByTeacherId(UUID teacherId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                assessment.id,
                assessment.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM AssessmentScheduleJpaEntity sch
            JOIN AssessmentScheduleProctorJpaEntity proctor
                ON proctor.scheduleId = sch.id AND proctor.teacherId = :teacherId
            JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE sch.status NOT IN :hiddenStatuses
                AND assessment.status <> 'DRAFT'
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("teacherId", teacherId)
            .setParameter("hiddenStatuses", HIDDEN_SCHEDULE_STATUSES)
            .getResultList();
    }

    @Override
    public List<ProctorScheduleSummary> findBySchoolId(UUID schoolId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                assessment.id,
                assessment.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM AssessmentScheduleJpaEntity sch
            JOIN AssessmentJpaEntity assessment ON assessment.id = sch.assessmentId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE assessment.schoolId = :schoolId
                AND sch.status NOT IN :hiddenStatuses
                AND assessment.status <> 'DRAFT'
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("schoolId", schoolId)
            .setParameter("hiddenStatuses", HIDDEN_SCHEDULE_STATUSES)
            .getResultList();
    }
}
