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

    /**
     * Ca thi của nhiều kỳ thi cùng lúc (đã loại các ca DELETED) — dùng cho DataLoader
     * {@code examSchedulesByExamId} để trang danh sách kỳ thi dựng được thanh tiến độ mà không N+1.
     */
    List<ExamSchedule> findByExamIdIn(Collection<UUID> examIds);

    /** Danh sách ca thi của toàn trường (đã loại các ca DELETED). */
    List<ExamSchedule> findBySchoolId(UUID schoolId);

    /** Tìm nhiều ca thi theo id (dùng cho DataLoader batch). */
    List<ExamSchedule> findByIdIn(java.util.Collection<UUID> ids);

    /**
     * Id của MỌI ca thi thuộc kỳ thi, kể cả ca đã xoá mềm — dùng khi dọn dữ liệu con lúc xoá hẳn
     * kỳ thi. {@link #findByExamId(UUID)} lọc bỏ DELETED nên dùng nó ở đó sẽ bỏ sót giám thị của
     * những ca đã xoá mềm.
     */
    List<UUID> findAllIdsByExamId(UUID examId);

    /** Xoá cứng mọi ca thi của kỳ thi (kể cả ca đã xoá mềm). */
    void deleteByExamId(UUID examId);

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
    /**
     * Ca đã công bố nhưng đã qua giờ kết thúc, cũ nhất trước, tối đa {@code limit} dòng — dùng cho
     * job tự chuyển ca sang COMPLETED. Có trần vì lần chạy đầu sau khi triển khai phải quét cả tồn
     * đọng lịch sử.
     */
    List<ExamSchedule> findPublishedEndedBefore(Instant now, int limit);

    List<ExamSchedule> findByExamIdAndInSchedule(UUID examId, Instant now);
    List<ExamSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now);
    Optional<ExamSchedule> findByIdAndInSchedule(UUID id, Instant now);
    List<ExamSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);
}
