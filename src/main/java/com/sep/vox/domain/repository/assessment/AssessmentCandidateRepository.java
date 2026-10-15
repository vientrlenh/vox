package com.sep.vox.domain.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidate;

public interface AssessmentCandidateRepository {
    AssessmentCandidate save(AssessmentCandidate candidate);
    List<AssessmentCandidate> saveAll(Collection<AssessmentCandidate> candidates);
    List<AssessmentCandidate> findByExamId(UUID examId);
    List<AssessmentCandidate> findByExamIdIn(Collection<UUID> examIds);
    long countByExamId(UUID examId);
    Map<UUID, Long> countByExamIdIn(Collection<UUID> examIds);

    void deleteById(UUID id);

    /** Xoá cả nhóm trong một câu lệnh — dùng cho thao tác xoá hàng loạt trên màn danh sách thí sinh. */
    void deleteByIdIn(Collection<UUID> ids);
    void deleteByExamId(UUID examId);

    Optional<AssessmentCandidate> findById(UUID id);
    Optional<AssessmentCandidate> findByExamIdAndStudentId(UUID examId, UUID studentId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);
    List<AssessmentCandidate> findByStudentId(UUID studentId);
    List<AssessmentCandidate> findByScheduleId(UUID scheduleId);
    /** Thí sinh đang được phân đúng mã đề này — dùng để gỡ phân đề trước khi xoá mã đề. */
    List<AssessmentCandidate> findByAssignedPaperId(UUID paperId);
    Set<UUID> findStudentIdsByExamId(UUID examId);
    List<UUID> findUnblockedStudentIdsByExamId(UUID examId);
    List<AssessmentCandidate> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId);
    List<AssessmentCandidate> findByIdIn(Collection<UUID> ids);
    List<AssessmentCandidate> findByIdInAndExamId(Collection<UUID> ids, UUID examId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId);
    Optional<AssessmentCandidate> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    List<AssessmentCandidate> findActiveCandidates(UUID studentId, Instant now);

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

