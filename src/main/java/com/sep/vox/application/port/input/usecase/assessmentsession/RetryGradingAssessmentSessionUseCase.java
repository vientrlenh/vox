package com.sep.vox.application.port.input.usecase.assessmentsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.RetryGradingAssessmentSessionCommand;
import com.sep.vox.application.port.input.command.SubmitAssessmentSessionCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;

@Service
public class RetryGradingAssessmentSessionUseCase implements IUseCase<RetryGradingAssessmentSessionCommand, java.util.UUID> {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final UserContextPort userContextPort;
    private final SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase;

    public RetryGradingAssessmentSessionUseCase(
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentRepository assessmentRepository,
            AssessmentSessionModerationAccessService moderationAccessService,
            UserContextPort userContextPort,
            SubmitAssessmentSessionUseCase submitAssessmentSessionUseCase) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentRepository = assessmentRepository;
        this.moderationAccessService = moderationAccessService;
        this.userContextPort = userContextPort;
        this.submitAssessmentSessionUseCase = submitAssessmentSessionUseCase;
    }

    @Override
    @Transactional
    public java.util.UUID execute(RetryGradingAssessmentSessionCommand input) {
        var session = assessmentSessionRepository.findById(input.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi"));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của phiên thi"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của phiên thi"));

        moderationAccessService.authorize(assessment, candidate);
        if (assessment.getStatus() == AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Kỳ thi đã công bố điểm, không thể chấm lại");
        }
        // G.4: ngoài GRADING_FAILED (lỗi chấm kỹ thuật) còn cho phép chấm AI lần đầu cho 1
        // session từng bị đánh dấu vi phạm oan (INVALID) - đã được dỡ cấm - và CHƯA từng có
        // AssessmentItemEvaluation nào (nếu đã từng có, phải recompute từ dữ liệu cũ thay vì gọi AI
        // lại, xem UnblockAssessmentCandidateUseCase).
        var result = assessmentCandidateResultRepository.findBySessionId(session.getId()).orElse(null);
        var isUnblockedInvalidRetry = result != null
            && result.getStatus() == AssessmentCandidateResultStatus.INVALID
            && candidate.getBlockedAt() == null;
        if (session.getStatus() != AssessmentSessionStatus.GRADING_FAILED && !isUnblockedInvalidRetry) {
            throw new IllegalStateException("Chỉ có thể chấm lại phiên thi đang lỗi chấm");
        }

        // Định mức chỉ áp cho phía TRƯỜNG, và chỉ cho lượt chấm lại thật (phiên đang lỗi chấm).
        //
        // Quản trị hệ thống không tiêu vào định mức này: lượt của họ là lượt khắc phục sự cố nền
        // tảng, và nếu nó đốt định mức của trường thì một lần AI hỏng hàng loạt sẽ ép hàng trăm bài
        // sang chấm tay vì lỗi của chính mình. Cùng lý do sẽ áp cho job tự chạy lại sau này.
        //
        // Nhánh INVALID được dỡ cấm KHÔNG tính: đó là lần chấm AI ĐẦU TIÊN của phiên, không phải
        // chấm lại sau khi hỏng.
        var schoolInitiated = !userContextPort.isSystemAdmin();
        if (schoolInitiated && session.getStatus() == AssessmentSessionStatus.GRADING_FAILED) {
            if (!session.canSchoolRequestAiRegrade()) {
                throw new IllegalStateException(
                    "Đã dùng hết lượt nhờ AI chấm lại cho phiên thi này, vui lòng chuyển sang chấm tay"
                );
            }
            session.setSchoolRegradeCount(session.getSchoolRegradeCount() + 1);
            assessmentSessionRepository.save(session);
        }

        submitAssessmentSessionUseCase.execute(new SubmitAssessmentSessionCommand(session.getId()));
        return session.getId();
    }
}
