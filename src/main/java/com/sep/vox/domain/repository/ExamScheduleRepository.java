package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamSchedule;

public interface ExamScheduleRepository {
    Optional<ExamSchedule> findById(UUID id);

    /**
     * (gán thí sinh thủ công / auto-fill). Dùng trong cùng một transaction.
     */
    Optional<ExamSchedule> findByIdForUpdate(UUID id);

    /** Danh sách ca thi của một bài kiểm tra (đã loại các ca DELETED). */
    List<ExamSchedule> findByExamId(UUID examId);

    /** Danh sách ca thi của toàn trường (đã loại các ca DELETED). */
    List<ExamSchedule> findBySchoolId(UUID schoolId);

    /** Tìm nhiều ca thi theo id (dùng cho DataLoader batch). */
    List<ExamSchedule> findByIdIn(java.util.Collection<UUID> ids);

    ExamSchedule save(ExamSchedule schedule);

    /**
     * Có ca thi nào (DRAFT/PUBLISHED) trùng phòng và giao thời gian với khoảng [start, end) hay không.
     * {@code excludeScheduleId} (nếu khác null) được loại khỏi phép kiểm tra (dùng khi sửa chính ca đó).
     */
    boolean existsOverlapping(UUID schoolRoomId, Instant start, Instant end, UUID excludeScheduleId);

    /**
     * Cập nhật nguyên tử phòng/giờ của ca thi. Chỉ áp dụng khi ca đang ở DRAFT (WHERE ... status='DRAFT').
     * Trả về số dòng bị ảnh hưởng.
     */
    int updateAtomic(UUID id, UUID schoolRoomId, Instant start, Instant end,
            Instant now, UUID updatedBy);
    List<ExamSchedule> findByExamIdAndInSchedule(UUID examId, Instant now);
    List<ExamSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now);
    Optional<ExamSchedule> findByIdAndInSchedule(UUID id, Instant now);
    List<ExamSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);
}
