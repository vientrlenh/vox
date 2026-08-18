package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamScheduleProctor;

public interface ExamScheduleProctorRepository {
    ExamScheduleProctor save(ExamScheduleProctor proctor);
    Optional<ExamScheduleProctor> findById(UUID id);
    List<ExamScheduleProctor> findByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndTeacherId(UUID scheduleId, UUID teacherId);
    long countByScheduleId(UUID scheduleId);
    void deleteById(UUID id);

    void deleteByScheduleIdIn(Collection<UUID> scheduleIds);
    List<UUID> findScheduleIdsByTeacherIdAndScheduleIdIn(UUID teacherId, Collection<UUID> scheduleIds);

    /**
     * Giáo viên này đã gác một ca thi (DRAFT/PUBLISHED) nào khác giao thời gian với [start, end) chưa.
     * Quét MỌI kỳ thi chứ không riêng kỳ thi đang mở: một người không thể có mặt ở hai ca cùng lúc.
     * {@code excludeScheduleId} (nếu khác null) được loại khỏi phép kiểm tra.
     */
    boolean existsOverlappingAssignment(UUID teacherId, Instant start, Instant end, UUID excludeScheduleId);

    /**
     * Với mỗi giáo viên trong {@code teacherIds}, ca thi (DRAFT/PUBLISHED) đầu tiên mà họ đã gác và
     * giao thời gian với [start, end) — dùng cho màn chọn giám thị để báo trước ai đang bận.
     * Giáo viên rảnh thì không có mặt trong kết quả.
     */
    List<ProctorScheduleConflict> findConflictsForTeachers(
            Collection<UUID> teacherIds, Instant start, Instant end, UUID excludeScheduleId);

    /** Một lần vướng lịch: giáo viên {@code teacherId} đã gác ca {@code scheduleId} chạy [start, end). */
    record ProctorScheduleConflict(UUID teacherId, UUID scheduleId, Instant startDate, Instant endDate) {
    }

    boolean existsByExamIdAndTeacherId(UUID examId, UUID teacherId);
}
