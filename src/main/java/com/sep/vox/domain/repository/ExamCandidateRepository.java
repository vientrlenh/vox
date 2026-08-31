package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamCandidate;

public interface ExamCandidateRepository {
    ExamCandidate save(ExamCandidate candidate);
    List<ExamCandidate> saveAll(Collection<ExamCandidate> candidates);
    List<ExamCandidate> findByExamId(UUID examId);
    List<ExamCandidate> findByExamIdIn(Collection<UUID> examIds);
    long countByExamId(UUID examId);
    Map<UUID, Long> countByExamIdIn(Collection<UUID> examIds);

    void deleteById(UUID id);

    /** Xoá cả nhóm trong một câu lệnh — dùng cho thao tác xoá hàng loạt trên màn danh sách thí sinh. */
    void deleteByIdIn(Collection<UUID> ids);
    void deleteByExamId(UUID examId);

    Optional<ExamCandidate> findById(UUID id);
    Optional<ExamCandidate> findByExamIdAndStudentId(UUID examId, UUID studentId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);
    List<ExamCandidate> findByStudentId(UUID studentId);
    List<ExamCandidate> findByScheduleId(UUID scheduleId);
    /** Thí sinh đang được phân đúng mã đề này — dùng để gỡ phân đề trước khi xoá mã đề. */
    List<ExamCandidate> findByAssignedPaperId(UUID paperId);
    Set<UUID> findStudentIdsByExamId(UUID examId);
    List<UUID> findUnblockedStudentIdsByExamId(UUID examId);
    List<ExamCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId);
    List<ExamCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId);
    Optional<ExamCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    List<ExamCandidate> findActiveCandidates(UUID studentId, Instant now);

    /**
     * Với mỗi học sinh trong {@code studentIds}, ca thi (DRAFT/PUBLISHED) mà họ đã được xếp và giao
     * thời gian với [start, end). Quét MỌI kỳ thi chứ không riêng kỳ thi đang mở: một học sinh không
     * thể ngồi ở hai phòng cùng lúc. {@code excludeScheduleId} (nếu khác null) được loại khỏi phép
     * kiểm tra. Học sinh rảnh thì không có mặt trong kết quả.
     */
    List<StudentScheduleConflict> findConflictsForStudents(
            Collection<UUID> studentIds, Instant start, Instant end, UUID excludeScheduleId);

    /** Một lần vướng lịch: học sinh {@code studentId} đã được xếp ca {@code scheduleId} chạy [start, end). */
    record StudentScheduleConflict(UUID studentId, UUID scheduleId, Instant startDate, Instant endDate) {
    }
}

