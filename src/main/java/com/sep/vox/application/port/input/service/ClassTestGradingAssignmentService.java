package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentGradingRoundType;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentMemberRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;

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

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;

    public ClassTestGradingAssignmentService(
            AssessmentRepository assessmentRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentMemberRepository = assessmentMemberRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentGradingAssignmentRepository = assessmentGradingAssignmentRepository;
    }

    /**
     * Mở phân công cho MỘT bài vừa rơi vào {@code PENDING_REVIEW}.
     *
     * <p>{@code REQUIRED} chứ không {@code MANDATORY}: điểm móc chính
     * ({@code RecordAssessmentAttemptEvaluationUseCase}) cố ý KHÔNG chạy trong một
     * transaction bao ngoài — nó tự chia phase bằng {@code TransactionTemplate}. Đổi
     * sang {@code MANDATORY} là chỗ đó ném ngay lúc chạy.
     *
     * <p>Hệ quả: phân công có thể commit ở transaction riêng, tách khỏi transaction
     * ghi kết quả. Chấp nhận được vì hàm này idempotent, và lượt quét bù lúc đóng bài
     * ({@link #ensureAssignmentsForAssessment}) chính là lưới hứng nếu tiến trình chết ở giữa.
     */
    @Transactional
    public void ensureAssignmentForResult(AssessmentCandidateResult result) {
        if (result == null || result.getStatus() != AssessmentCandidateResultStatus.PENDING_REVIEW) {
            return;
        }
        var chairId = findClassTestChairId(result.getAssessmentId());
        if (chairId == null) {
            return;
        }
        if (assessmentGradingAssignmentRepository.findOpenByCandidateResultId(result.getId()).isPresent()) {
            return;
        }
        assessmentGradingAssignmentRepository.save(open(result, chairId, Instant.now()));
    }

    /**
     * Quét bù toàn bộ bài {@code PENDING_REVIEW} của một bài kiểm tra trên lớp. Dùng
     * lúc đóng bài, nơi vẫn còn kết quả sinh muộn ngoài luồng thường: bài do job chấm
     * trễ, bài vừa được gỡ vô hiệu.
     *
     * <p>Bài KHÔNG ở {@code PENDING_REVIEW} cố ý nằm ngoài lượt quét này: bài AI chấm
     * sạch và bài vắng/trống cho điểm 0 đều đi thẳng sang {@code RELEASED}, mở phân công
     * tự động cho chúng là nhét vào hàng đợi hàng loạt bài không ai cần chấm. Giáo viên
     * vẫn thấy và nhận chấm được chúng ở màn danh sách bài
     * ({@code classTestGradingResults} → {@code ClaimClassTestGradingUseCase}), qua vòng
     * hậu kiểm thay vì vòng đầu.
     *
     * @return số phân công vừa mở
     */
    @Transactional
    public int ensureAssignmentsForAssessment(UUID assessmentId) {
        if (assessmentId == null) {
            return 0;
        }
        var chairId = findClassTestChairId(assessmentId);
        if (chairId == null) {
            return 0;
        }
        var pending = assessmentCandidateResultRepository.findByAssessmentId(assessmentId).stream()
            .filter(result -> result.getStatus() == AssessmentCandidateResultStatus.PENDING_REVIEW)
            .toList();
        if (pending.isEmpty()) {
            return 0;
        }

        // Một query cho cả lô thay vì hỏi từng bài — số bài bằng sĩ số lớp.
        var alreadyOpen = assessmentGradingAssignmentRepository
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
        return assessmentGradingAssignmentRepository.saveAll(toOpen).size();
    }

    /** {@code null} = không phải bài trên lớp, hoặc bài không có CHAIR. */
    private UUID findClassTestChairId(UUID assessmentId) {
        var assessment = assessmentRepository.findById(assessmentId).orElse(null);
        if (assessment == null || assessment.getKind() != AssessmentKind.CLASS_TEST) {
            return null;
        }
        return assessmentMemberRepository.findByAssessmentId(assessmentId).stream()
            .filter(member -> member.getRole() == AssessmentMemberRole.CHAIR)
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
    private AssessmentGradingAssignment open(AssessmentCandidateResult result, UUID chairId, Instant now) {
        return AssessmentGradingAssignment.open(
            result.getId(),
            chairId,
            AssessmentGradingRoundType.INITIAL,
            null,
            result.getTotalScore(),
            now,
            chairId,
            null
        );
    }
}
