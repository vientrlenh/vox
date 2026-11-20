package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.HandOffGradingToHumanCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.repository.AssessmentPolicyRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Bài AI chấm lỗi -> giao cho người chấm.
 *
 * <p><strong>Vì sao cần một use case riêng thay vì dùng lại phân công chấm:</strong> phân công
 * chấm gắn vào {@code exam_candidate_results}, mà phiên {@code GRADING_FAILED} <em>không có dòng
 * kết quả nào</em> — đo được 2026-08-18 trên phiên 01a015a8: 2 câu trả lời, 0 bản chấm,
 * {@code result_id} null. Không có kết quả thì không có gì để giao, nên hàng đợi chấm không bao
 * giờ thấy bài này và nó nằm lại vĩnh viễn ở màn "Chấm điểm thất bại".
 *
 * <p>Việc duy nhất ở đây là <em>tạo ra bài để giao</em>: một dòng kết quả {@code PENDING_REVIEW}
 * chưa có điểm. {@code GradingRoundPolicy.assignableStatuses(INITIAL)} đúng bằng
 * {@code {PENDING_REVIEW}}, nên ngay sau đó bài xuất hiện trong hàng đợi ở dạng CHƯA PHÂN CÔNG
 * (vị từ {@code unassignedOnly} của JpaExamGradingQueryRepository là "không có phân công nào trỏ
 * tới kết quả này") và nhà trường giao cho giáo viên như mọi bài khác.
 *
 * <p><strong>Không tự chọn giáo viên:</strong> giao cho ai là quyết định điều phối của nhà
 * trường — ai đang rảnh, ai dạy lớp đó, ai đã chấm bài này vòng trước. Đoán hộ ở đây là đưa bài
 * cho một người có thể không nên nhận nó.
 *
 * <p><strong>KHÔNG chấm 0 và KHÔNG đổi trạng thái phiên.</strong> Phiên giữ nguyên
 * {@code GRADING_FAILED} vì đó là sự thật: AI đã chấm lỗi. Nhờ vậy nút chấm lại bằng AI vẫn còn
 * dùng được — hai lối ra không loại trừ nhau, người dùng có thể thử AI lại sau khi đã đưa vào
 * hàng đợi người chấm.
 */
@Service
public class HandOffGradingToHumanUseCase implements IUseCase<HandOffGradingToHumanCommand, UUID> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandOffGradingToHumanUseCase.class);

    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final AssessmentPolicyRepository assessmentPolicyRepository;
    private final ExamSessionModerationAccessService moderationAccessService;

    public HandOffGradingToHumanUseCase(
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamItemResponseRepository examItemResponseRepository,
            AssessmentPolicyRepository assessmentPolicyRepository,
            ExamSessionModerationAccessService moderationAccessService) {
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.assessmentPolicyRepository = assessmentPolicyRepository;
        this.moderationAccessService = moderationAccessService;
    }

    @Override
    @Transactional
    public UUID execute(HandOffGradingToHumanCommand input) {
        var session = examSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(exam, candidate);
        if (exam.getStatus() == ExamStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố điểm, không thể chuyển sang chấm tay");
        }
        if (session.getStatus() != ExamSessionStatus.GRADING_FAILED) {
            throw new IllegalStateException("Chỉ chuyển sang chấm tay được khi AI chấm lỗi");
        }

        // Không có câu trả lời thì không có gì để người chấm xem. Giao một bài trắng chỉ tạo ra
        // một việc không làm được: giáo viên mở ra, không thấy gì, và cũng không đóng được phân
        // công vì không chấm nổi.
        if (examItemResponseRepository.findBySessionId(session.getId()).isEmpty()) {
            throw new IllegalStateException(
                "Phiên thi này không có câu trả lời nào, không có gì để người chấm xem");
        }

        var existing = examCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        if (existing != null) {
            // Đã có kết quả nghĩa là bài đã ở trong vòng đời chấm rồi -- vào hàng đợi hoặc đang
            // được giao. Tạo thêm là sinh bài trùng; đổi trạng thái của nó là ghi đè quyết định
            // của người khác.
            LOGGER.info(
                "Phiên {} đã có kết quả {} ({}), không tạo lại.",
                session.getId(), existing.getId(), existing.getStatus());
            return existing.getId();
        }

        var policy = exam.getAssessmentPolicyId() == null
            ? null
            : assessmentPolicyRepository.findById(exam.getAssessmentPolicyId()).orElse(null);
        var now = Instant.now();

        // Cùng bộ field với persistInvalidBlockedResult (SubmitExamSessionUseCase): rubric và
        // framework phải được chốt NGAY lúc tạo, vì màn chấm đọc thang điểm và danh sách tiêu chí
        // từ chính rubricVersionId của kết quả. Để null thì giáo viên mở ra không có tiêu chí nào
        // để nhập.
        var result = new ExamCandidateResult();
        result.setExamId(session.getExamId());
        result.setCandidateId(session.getCandidateId());
        result.setSessionId(session.getId());
        result.setAssessmentPolicyId(policy == null ? null : policy.getId());
        result.setPolicyVersion(policy == null ? 0 : policy.getVersion());
        result.setRubricVersionId(policy == null ? null : policy.getRubricVersionId());
        result.setFrameworkVersionId(policy == null ? null : policy.getFrameworkVersionId());
        result.setTargetFrameworkBandId(policy == null ? null : policy.getTargetFrameworkBandId());
        result.setRubricResultBandId(null);
        // Điểm để TRỐNG, không phải 0. Chưa ai chấm thì chưa có điểm, và 0 ở đây sẽ đi thẳng vào
        // bảng điểm như một con điểm thật.
        result.setTotalScore(null);
        result.setStatus(ExamCandidateResultStatus.PENDING_REVIEW);
        result.setCreatedAt(now);
        result.setUpdatedAt(now);
        result.setUpdatedBy(moderationAccessService.getCurrentUserId());

        var saved = examCandidateResultRepository.save(result);
        LOGGER.info(
            "Phiên {} (AI chấm lỗi) chuyển sang chấm tay: tạo kết quả {} ở PENDING_REVIEW, "
                + "chờ nhà trường phân công.",
            session.getId(), saved.getId());
        return saved.getId();
    }
}
