package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentSchedule;

public interface AssessmentScheduleRepository {
    Optional<AssessmentSchedule> findById(UUID id);

    /**
     * (gán thí sinh thủ công / auto-fill). Dùng trong cùng một transaction.
     */
    Optional<AssessmentSchedule> findByIdForUpdate(UUID id);

    /** Danh sách ca thi của một bài kiểm tra (đã loại các ca DELETED). */
    List<AssessmentSchedule> findByAssessmentId(UUID assessmentId);

    /**
     * The sessions of many assessments at once (DELETED sessions excluded) -- used by the DataLoader
     * {@code assessmentSchedulesByAssessmentId} so the assessment list page can build its progress bar without N+1.
     */
    List<AssessmentSchedule> findByAssessmentIdIn(Collection<UUID> assessmentIds);

    /** Danh sách ca thi của toàn trường (đã loại các ca DELETED). */
    List<AssessmentSchedule> findBySchoolId(UUID schoolId);

    /** Tìm nhiều ca thi theo id (dùng cho DataLoader batch). */
    List<AssessmentSchedule> findByIdIn(java.util.Collection<UUID> ids);

    /**
     * The ids of EVERY session of the assessment, including soft-deleted ones -- used when cleaning up child data when hard-deleting
     * an assessment. {@link #findByAssessmentId(UUID)} filters out DELETED so using it there would miss the proctors of
     * the soft-deleted sessions.
     */
    List<UUID> findAllIdsByAssessmentId(UUID assessmentId);

    /** Xoá cứng mọi ca thi của kỳ thi (kể cả ca đã xoá mềm). */
    void deleteByAssessmentId(UUID assessmentId);

    AssessmentSchedule save(AssessmentSchedule schedule);

    /**
     * Có ca thi nào (DRAFT/PUBLISHED) trùng phòng và giao thời gian với khoảng [start, end) hay không.
     * {@code excludeScheduleId} (nếu khác null) được loại khỏi phép kiểm tra (dùng khi sửa chính ca đó).
     */
    boolean existsOverlapping(UUID schoolRoomId, Instant start, Instant end, UUID excludeScheduleId);

    /**
     * Atomically updates the room/time of a schedule. Applied only while the schedule is DRAFT (WHERE ... status='DRAFT').
     * Returns the number of affected rows. The adapter writes the audit values itself, because a bulk update bypasses
     * the auditing listener.
     */
    int updateAtomic(UUID id, UUID schoolRoomId, boolean schoolRoomIdProvided, Instant start, Instant end);
    /**
     * Ca đã công bố nhưng đã qua giờ kết thúc, cũ nhất trước, tối đa {@code limit} dòng — dùng cho
     * job tự chuyển ca sang COMPLETED. Có trần vì lần chạy đầu sau khi triển khai phải quét cả tồn
     * đọng lịch sử.
     */
    List<AssessmentSchedule> findPublishedEndedBefore(Instant now, int limit);

    List<AssessmentSchedule> findByAssessmentIdAndInSchedule(UUID assessmentId, Instant now);
    List<AssessmentSchedule> findByIdInAndInSchedule(Collection<UUID> ids, Instant now);
    Optional<AssessmentSchedule> findByIdAndInSchedule(UUID id, Instant now);
    List<AssessmentSchedule> findByIdInAndInScheduleAndSchoolId(Collection<UUID> ids, Instant now, UUID schoolId);
}
