package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;

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
     * Has this teacher ever been assigned to grade that attempt -- any round, any status.
     *
     * <p>Deliberately counts {@code COMPLETED} rows too: for an appealed attempt, the grader of the previous round
     * is the one being asked again, and by then their assignment row is closed. Used for READ-ONLY checks
     * (see the assessment recording); every score WRITE action still has to go through
     * {@code AssessmentGradingAccessService.authorizeAssignedTeacher} on the row that is actually open.
     */
    boolean existsByCandidateResultIdAndTeacherId(UUID candidateResultId, UUID teacherId);

    /** Vòng phúc khảo của một đơn (mọi trạng thái). */
    List<AssessmentGradingAssignment> findByAppealId(UUID appealId);

    /** Phân công còn mở đã quá hạn — nguồn của job nhắc và của thu hồi hàng loạt. */
    List<AssessmentGradingAssignment> findOverdue(Instant now);

    /**
     * Overdue within the scope of one school (and one assessment if {@code assessmentId != null}), oldest
     * first, at most {@code limit} rows. Bulk reclaiming uses this so it does not
     * touch another school's data and so one pass does not pull the whole school into a single write transaction.
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

    long countByTeacherIdAndStatus(UUID teacherId, GradingAssignmentStatus status);
}
