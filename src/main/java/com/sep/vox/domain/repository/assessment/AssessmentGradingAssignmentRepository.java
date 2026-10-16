package com.sep.vox.domain.repository.assessment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignmentStatus;

public interface AssessmentGradingAssignmentRepository {
    Optional<AssessmentGradingAssignment> findById(UUID id);

    /**
     * Như {@link #findById} nhưng khoá dòng để ghi, khoá giữ tới hết transaction. Chỉ
     * gọi TRONG một transaction ghi — dùng cho các luồng đọc-sửa-ghi trên cùng một phân
     * công (chấm bài, đổi giáo viên, đặt hạn) để chúng nối đuôi nhau thay vì ghi đè nhau.
     */
    Optional<AssessmentGradingAssignment> findByIdForUpdate(UUID id);

    /**
     * Phân công đang mở của một bài. Một bài tối đa một dòng mở — DB enforce bằng
     * unique index trên {@code active_result_id}; các dòng đã đóng của vòng trước
     * mang {@code active_result_id = NULL} nên không lọt vào đây.
     */
    Optional<AssessmentGradingAssignment> findOpenByCandidateResultId(UUID candidateResultId);

    List<AssessmentGradingAssignment> findOpenByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Toàn bộ lịch sử phân công của một bài, mới nhất trước. */
    List<AssessmentGradingAssignment> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<AssessmentGradingAssignment> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /**
     * Giáo viên này có từng được giao chấm bài đó không — mọi vòng, mọi trạng thái.
     *
     * <p>Cố ý đếm cả dòng đã {@code COMPLETED}: bài bị phúc khảo thì chính người chấm vòng trước
     * là người bị hỏi lại, mà lúc đó dòng phân công của họ đã đóng. Dùng cho các kiểm tra CHỈ
     * ĐỌC (xem bản ghi buổi thi); mọi hành động GHI điểm vẫn phải đi qua
     * {@code AssessmentGradingAccessService.authorizeAssignedTeacher} trên đúng dòng đang mở.
     */
    boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId);

    /** Vòng phúc khảo của một đơn (mọi trạng thái). */
    List<AssessmentGradingAssignment> findByAppealId(UUID appealId);

    /** Phân công còn mở đã quá hạn — nguồn của job nhắc và của thu hồi hàng loạt. */
    List<AssessmentGradingAssignment> findOverdue(Instant now);

    /**
     * Quá hạn trong phạm vi một trường (và một kỳ thi nếu {@code assessmentId != null}), cũ
     * nhất trước, tối đa {@code limit} dòng. Thu hồi hàng loạt dùng cái này để không
     * chạm dữ liệu trường khác và để một lượt không ôm cả trường vào một transaction ghi.
     */
    List<AssessmentGradingAssignment> findOverdueInSchool(
        Instant now, UUID schoolId, UUID assessmentId, int limit);

    /** Quá hạn và chưa từng gửi mail nhắc. Chống nhắc trùng bằng {@code reminded_at}. */
    List<AssessmentGradingAssignment> findDueForReminder(Instant threshold);

    AssessmentGradingAssignment save(AssessmentGradingAssignment assignment);

    List<AssessmentGradingAssignment> saveAll(List<AssessmentGradingAssignment> assignments);

    void deleteById(UUID id);

    /** Dọn theo bài khi xoá phiên thi — không có FK nào chặn, bỏ sót là để lại dòng mồ côi. */
    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    long countByTeacherIdAndStatus(UUID teacherId, AssessmentGradingAssignmentStatus status);
}
