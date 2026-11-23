package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.application.port.input.command.RecordAiUsageCommand;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReportAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.metering.RecordAiUsageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Receives AI cost reports from a source with no direct Kafka connection -- for example the desktop app that aggregates TTS itself
 * on the learner's machine (the desktop app, LocalAvatarSpeaker.cs) instead of going through the Agentic AI. This REST path is parallel to the
 * Kafka consumer AiUsageRecordedConsumer (sourced from the Agentic AI), and both
 * feed into RecordAiUsageUseCase to share the idempotency logic
 * (uk_ai_usage_record_usage_event_id).
 *
 * <p>Only accepts a report for the calling learner's OWN assessment session, and that session must still be
 * RESUMABLE (in progress) -- the same constraint as UpdateAssessmentSessionRemainingTimeUseCase, because a usage report
 * only makes sense while the session is running.
 */
@Service
@RequiredArgsConstructor
public class ReportAiUsageUseCase implements IUseCase<ReportAiUsageCommand, Void> {

    private final UserContextPort userContextPort;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final RecordAiUsageUseCase recordAiUsageUseCase;

    /**
     * CỐ Ý KHÔNG @Transactional: mỗi usage event phải đứng độc lập, đúng như đường Kafka
     * (AiUsageRecordedConsumer không @Transactional nên vòng lặp của nó cũng cho mỗi event một
     * transaction riêng của RecordAiUsageUseCase).
     *
     * <p>Gói cả lô vào một transaction thì một event hỏng sẽ kéo theo những event ĐÃ GHI ĐƯỢC trong
     * cùng request. Nguồn gọi đường này là desktop app, báo cáo kiểu best-effort và KHÔNG retry
     * (LocalAvatarSpeaker.ReportTtsUsageBestEffortAsync nuốt lỗi rồi thôi) -- lô bị loại là chi phí
     * thật biến mất vĩnh viễn, không có lần thử thứ hai nào.
     *
     * <p>Hai bước kiểm tra quyền bên dưới chỉ ĐỌC nên không cần transaction bao ngoài.
     */
    @Override
    public Void execute(ReportAiUsageCommand input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();

        AssessmentSession session = assessmentSessionRepository.findByIdAndResumable(input.assessmentSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã kết thúc"));

        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        for (RecordAiUsageCommand item : input.usageEvents()) {
            recordAiUsageUseCase.execute(item);
        }
        return null;
    }
}
