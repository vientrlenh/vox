package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.ProctorScheduleSummary;
import com.sep.vox.application.query.repository.ProctorScheduleQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaProctorScheduleQueryRepository implements ProctorScheduleQueryRepository {

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
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("teacherId", teacherId)
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
            ORDER BY sch.startDate DESC
        """, ProctorScheduleSummary.class)
            .setParameter("schoolId", schoolId)
            .getResultList();
    }
}
