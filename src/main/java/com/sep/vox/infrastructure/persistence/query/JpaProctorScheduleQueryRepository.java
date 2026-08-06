package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;
import com.sep.vox.domain.model.exam.ExamScheduleStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaProctorScheduleQueryRepository implements ProctorScheduleQueryRepository {

    /**
     * Ca đã xoá mềm hoặc đã dời sang ca khác không còn là ca thật, giám thị không được thấy chúng
     * trong màn điểm danh. CANCELLED vẫn hiện kèm trạng thái để giám thị biết ca bị huỷ.
     */
    private static final Set<String> INACTIVE_STATUSES = Set.of(
        ExamScheduleStatus.DELETED.name(),
        ExamScheduleStatus.MOVED.name());

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ProctorScheduleSummary> findByTeacherId(UUID teacherId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                exam.id,
                exam.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM ExamScheduleJpaEntity sch
            JOIN ExamScheduleProctorJpaEntity proctor
                ON proctor.scheduleId = sch.id AND proctor.teacherId = :teacherId
            JOIN ExamJpaEntity exam ON exam.id = sch.examId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE sch.status NOT IN :inactiveStatuses
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("teacherId", teacherId)
            .setParameter("inactiveStatuses", INACTIVE_STATUSES)
            .getResultList();
    }

    @Override
    public List<ProctorScheduleSummary> findBySchoolId(UUID schoolId) {
        return em.createQuery("""
            SELECT NEW com.sep.vox.application.query.dto.ProctorScheduleSummary(
                sch.id,
                exam.id,
                exam.name,
                room.id,
                room.name,
                sch.startDate,
                sch.endDate,
                sch.status
            )
            FROM ExamScheduleJpaEntity sch
            JOIN ExamJpaEntity exam ON exam.id = sch.examId
            LEFT JOIN SchoolRoomJpaEntity room ON room.id = sch.schoolRoomId
            WHERE exam.schoolId = :schoolId
                AND sch.status NOT IN :inactiveStatuses
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("schoolId", schoolId)
            .setParameter("inactiveStatuses", INACTIVE_STATUSES)
            .getResultList();
    }
}
