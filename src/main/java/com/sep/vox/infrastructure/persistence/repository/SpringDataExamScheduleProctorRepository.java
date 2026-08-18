package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamScheduleProctorJpaEntity;

public interface SpringDataExamScheduleProctorRepository extends JpaRepository<ExamScheduleProctorJpaEntity, UUID> {
    List<ExamScheduleProctorJpaEntity> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);

    List<ExamScheduleProctorJpaEntity> findByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);
    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);

    /**
     * Đếm ca thi còn hiệu lực mà giáo viên đã gác và giao thời gian với [start, end).
     *
     * <p>Giờ nằm ở bảng cha nên phải join sang {@code exam_schedules}. Điều kiện giao khoảng dùng
     * đúng dạng nửa mở như {@code SpringDataExamScheduleRepository.countOverlapping}: hai ca kề nhau
     * (ca trước kết thúc đúng lúc ca sau bắt đầu) KHÔNG tính là trùng.
     */
    @Query("""
        SELECT COUNT(p) FROM ExamScheduleProctorJpaEntity p
        JOIN ExamScheduleJpaEntity s ON s.id = p.scheduleId
        WHERE p.teacherId = :teacherId
          AND s.status IN ('DRAFT', 'PUBLISHED')
          AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
          AND s.startDate < :end AND s.endDate > :start
        """)
    long countOverlappingAssignments(
        @Param("teacherId") UUID teacherId,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("excludeScheduleId") UUID excludeScheduleId);

    /** Cùng phép lọc như trên nhưng cho cả một nhóm giáo viên, và trả về ca thi đang vướng. */
    @Query("""
        SELECT p.teacherId, s.id, s.startDate, s.endDate
        FROM ExamScheduleProctorJpaEntity p
        JOIN ExamScheduleJpaEntity s ON s.id = p.scheduleId
        WHERE p.teacherId IN :teacherIds
          AND s.status IN ('DRAFT', 'PUBLISHED')
          AND (:excludeScheduleId IS NULL OR s.id <> :excludeScheduleId)
          AND s.startDate < :end AND s.endDate > :start
        ORDER BY s.startDate
        """)
    List<Object[]> findOverlappingAssignments(
        @Param("teacherIds") Collection<UUID> teacherIds,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("excludeScheduleId") UUID excludeScheduleId);


    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM ExamScheduleProctorJpaEntity p
        JOIN ExamScheduleJpaEntity s ON s.id = p.scheduleId
        WHERE p.teacherId = :teacherId
          AND s.examId = :examId
          AND s.status NOT IN ('DELETED', 'MOVED')
        """)
    boolean existsByExamIdAndTeacherId(@Param("examId") UUID examId, @Param("teacherId") UUID teacherId);
}
