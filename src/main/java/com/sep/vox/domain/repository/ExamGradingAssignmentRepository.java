package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamGradingAssignment;

public interface ExamGradingAssignmentRepository {
    Optional<ExamGradingAssignment> findById(UUID id);

    /**
     * Phân công đang mở của một bài. Một bài tối đa một dòng mở — DB enforce bằng
     * unique index trên {@code active_result_id}; các dòng đã đóng của vòng trước
     * mang {@code active_result_id = NULL} nên không lọt vào đây.
     */
    Optional<ExamGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId);

    List<ExamGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Toàn bộ lịch sử phân công của một bài, mới nhất trước. */
    List<ExamGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<ExamGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Vòng phúc khảo của một đơn (mọi trạng thái). */
    List<ExamGradingAssignment> findByAppealId(UUID appealId);

    /** Phân công còn mở đã quá hạn — nguồn của job nhắc và của thu hồi hàng loạt. */
    List<ExamGradingAssignment> findOverdue(OffsetDateTime now);

    /**
     * Quá hạn trong phạm vi một trường (và một kỳ thi nếu {@code examId != null}).
     * Thu hồi hàng loạt dùng cái này để không chạm dữ liệu trường khác.
     */
    List<ExamGradingAssignment> findOverdueInSchool(OffsetDateTime now, UUID schoolId, UUID examId);

    /** Quá hạn và chưa từng gửi mail nhắc. Chống nhắc trùng bằng {@code reminded_at}. */
    List<ExamGradingAssignment> findDueForReminder(OffsetDateTime threshold);

    ExamGradingAssignment save(ExamGradingAssignment assignment);

    List<ExamGradingAssignment> saveAll(List<ExamGradingAssignment> assignments);

    void deleteById(UUID id);

    /** Dọn theo bài khi xoá phiên thi — không có FK nào chặn, bỏ sót là để lại dòng mồ côi. */
    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
