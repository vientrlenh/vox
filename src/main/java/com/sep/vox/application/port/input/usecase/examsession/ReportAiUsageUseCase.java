package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ReportAiUsageCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.metering.RecordAiUsageUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Nhận báo cáo chi phí AI từ nguồn không có kết nối Kafka trực tiếp -- ví dụ desktop app tự tổng
 * hợp TTS ngay trên máy học viên (VoxExamDesktop/LocalAvatarSpeaker.cs) thay vì qua Agentic AI.
 * Đường REST này song song với Kafka consumer AiUsageRecordedConsumer (nguồn từ Agentic AI), cả
 * hai cùng đổ vào RecordAiUsageUseCase để dùng chung logic idempotency
 * (uk_ai_usage_record_usage_event_id).
 *
 * <p>Chỉ chấp nhận báo cáo cho CHÍNH phiên thi của học viên đang gọi, và phiên đó phải còn
 * RESUMABLE (đang thi) -- cùng ràng buộc với UpdateExamSessionRemainingTimeUseCase, vì báo cáo
 * usage chỉ có ý nghĩa khi phiên thi đang diễn ra.
 */
@Service
public class ReportAiUsageUseCase implements IUseCase<ReportAiUsageCommand, Void> {

    private final UserContextPort userContextPort;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final RecordAiUsageUseCase recordAiUsageUseCase;

    public ReportAiUsageUseCase(
            UserContextPort userContextPort,
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            RecordAiUsageUseCase recordAiUsageUseCase) {
        this.userContextPort = userContextPort;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.recordAiUsageUseCase = recordAiUsageUseCase;
    }

    @Override
    @Transactional
    public Void execute(ReportAiUsageCommand input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var session = examSessionRepository.findByIdAndResumable(input.examSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi hoặc phiên thi đã kết thúc"));

        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy học viên thi"));

        if (!candidate.getStudentId().equals(userId)) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        for (var item : input.usageEvents()) {
            recordAiUsageUseCase.execute(item);
        }
        return null;
    }
}
