package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSchedule;

public interface ExamScheduleRepository {
    Optional<ExamSchedule> findById(UUID id);

    /** Danh sách ca thi của một bài kiểm tra (đã loại các ca DELETED). */
    List<ExamSchedule> findByExamId(UUID examId);

    ExamSchedule save(ExamSchedule schedule);

    /**
     * Có ca thi nào (DRAFT/PUBLISHED) trùng phòng và giao thời gian với khoảng [start, end) hay không.
     * {@code excludeScheduleId} (nếu khác null) được loại khỏi phép kiểm tra (dùng khi sửa chính ca đó).
     */
    boolean existsOverlapping(UUID schoolRoomId, OffsetDateTime start, OffsetDateTime end, UUID excludeScheduleId);

    /**
     * Cập nhật nguyên tử phòng/giờ của ca thi. Chỉ áp dụng khi ca đang ở DRAFT (WHERE ... status='DRAFT').
     * Trả về số dòng bị ảnh hưởng.
     */
    int updateAtomic(UUID id, UUID schoolRoomId, OffsetDateTime start, OffsetDateTime end,
            OffsetDateTime now, UUID updatedBy);
}
