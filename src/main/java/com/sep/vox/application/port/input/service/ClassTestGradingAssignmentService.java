package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.exam.ExamMemberRole;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamRepository;

/**
 * Bài kiểm tra trên lớp: chủ bài (CHAIR) chấm hết, nên phân công vòng
 * {@code INITIAL} được mở TỰ ĐỘNG thay vì chờ school admin gán — admin không quản
 * bài trên lớp, nên nếu chờ thì hàng đợi của giáo viên rỗng vĩnh viễn.
 *
 * <p>Kỳ thi {@code CENTRALIZED} không đi qua đây: mọi hàm đều no-op khi
 * {@code kind != CLASS_TEST}, nên luồng phân công của nhà trường giữ nguyên.
 *
 * <p><strong>Gọi được nhiều lần.</strong> Mọi đường ghi đều kiểm "bài đã có phân
 * công đang mở chưa" trước — đó là hàng rào chống vi phạm unique index
 * {@code uq_grading_assignment_active_result}. Nhờ vậy hai điểm móc (khi sinh kết
 * quả và khi đóng bài) chồng lên nhau cũng không sinh dòng thứ hai.
 */
@Service
public class ClassTestGradingAssignmentService {

    private final ExamRepository examRepository;
    private final ExamMemberRepository examMemberRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamGradingAssignmentRepository examGradingAssignmentRepository;

    public ClassTestGradingAssignmentService(
            ExamRepository examRepository,
            ExamMemberRepository examMemberRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamGradingAssignmentRepository examGradingAssignmentRepository) {
        this.examRepository = examRepository;
        this.examMemberRepository = examMemberRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examGradingAssignmentRepository = examGradingAssignmentRepository;
    }

    /**
     * Mở phân công cho MỘT bài vừa rơi vào {@code PENDING_REVIEW}.
     *
     * <p>{@code REQUIRED} chứ không {@code MANDATORY}: điểm móc chính
     * ({@code RecordExamAttemptEvaluationUseCase}) cố ý KHÔNG chạy trong một
     * transaction bao ngoài — nó tự chia phase bằng {@code TransactionTemplate}. Đổi
     * sang {@code MANDATORY} là chỗ đó ném ngay lúc chạy.
     *
     * <p>Hệ quả: phân công có thể commit ở transaction riêng, tách khỏi transaction
     * ghi kết quả. Chấp nhận được vì hàm này idempotent, và lượt quét bù lúc đóng bài
     * ({@link #ensureAssignmentsForExam}) chính là lưới hứng nếu tiến trình chết ở giữa.
     */
    @Transactional
    public void ensureAssignmentForResult(ExamCandidateResult result) {
        if (result == null || result.getStatus() != ExamCandidateResultStatus.PENDING_REVIEW) {
            return;
        }
        var chairId = findClassTestChairId(result.getExamId());
        if (chairId == null) {
            return;
        }
        if (examGradingAssignmentRepository.findOpenByCandidateResultId(result.getId()).isPresent()) {
            return;
        }
        examGradingAssignmentRepository.save(open(result, chairId, Instant.now()));
    }

    /**
     * Quét bù toàn bộ bài {@code PENDING_REVIEW} của một bài kiểm tra trên lớp. Dùng
     * lúc đóng bài, nơi vẫn còn kết quả sinh muộn ngoài luồng thường: bài vắng/trống
     * được cho điểm 0, bài do job chấm trễ, bài vừa được gỡ vô hiệu.
     *
     * @return số phân công vừa mở
     */
    @Transactional
    public int ensureAssignmentsForExam(UUID examId) {
        if (examId == null) {
            return 0;
        }
        var chairId = findClassTestChairId(examId);
        if (chairId == null) {
            return 0;
        }
        var pending = examCandidateResultRepository.findByExamId(examId).stream()
            .filter(result -> result.getStatus() == ExamCandidateResultStatus.PENDING_REVIEW)
            .toList();
        if (pending.isEmpty()) {
            return 0;
        }

        // Một query cho cả lô thay vì hỏi từng bài — số bài bằng sĩ số lớp.
        var alreadyOpen = examGradingAssignmentRepository
            .findOpenByCandidateResultIdIn(pending.stream().map(result -> result.getId()).toList()).stream()
            .map(assignment -> assignment.getCandidateResultId())
            .collect(Collectors.toSet());

        var now = Instant.now();
        var toOpen = pending.stream()
            .filter(result -> !alreadyOpen.contains(result.getId()))
            .map(result -> open(result, chairId, now))
            .toList();
        if (toOpen.isEmpty()) {
            return 0;
        }
        return examGradingAssignmentRepository.saveAll(toOpen).size();
    }

    /** {@code null} = không phải bài trên lớp, hoặc bài không có CHAIR. */
    private UUID findClassTestChairId(UUID examId) {
        var exam = examRepository.findById(examId).orElse(null);
        if (exam == null || exam.getKind() != ExamKind.CLASS_TEST) {
            return null;
        }
        return examMemberRepository.findByExamId(examId).stream()
            .filter(member -> member.getRole() == ExamMemberRole.CHAIR)
            .map(member -> member.getUserId())
            .findFirst()
            .orElse(null);
    }

    /**
     * {@code deadlineAt = null} — bài trên lớp không có hạn chấm hành chính, và
     * {@code GradingDeadlineReminderJob} chỉ bắn mail cho dòng CÓ hạn.
     *
     * <p>{@code assignedBy = chairId}: hệ thống mở thay, nhưng người chịu trách nhiệm
     * là chính họ.
     *
     * <p>{@code scoreBefore} chụp NGAY LÚC MỞ — đây là mốc đo độ lệch AI ↔ người; lấy
     * sau thì đã bị chính giáo viên sửa mất.
     */
    private ExamGradingAssignment open(ExamCandidateResult result, UUID chairId, Instant now) {
        return ExamGradingAssignment.open(
            result.getId(),
            chairId,
            GradingRoundType.INITIAL,
            null,
            result.getTotalScore(),
            now,
            chairId,
            null
        );
    }
}
