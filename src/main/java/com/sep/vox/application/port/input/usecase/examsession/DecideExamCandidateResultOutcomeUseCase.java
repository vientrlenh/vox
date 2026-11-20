package com.sep.vox.application.port.input.usecase.examsession;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DecideExamCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.service.ExamSessionModerationAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamStatus;
import com.sep.vox.domain.model.exam.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * G.3 addendum: khi assessmentPolicy không có passingScore, finalizeForPublish chốt kết quả
 * ở FINAL thay vì tự suy ra PASSED/FAILED (không có ngưỡng để so sánh). Use case này cho phép
 * nhà trường tự quyết định thủ công FINAL -> PASSED hoặc FINAL -> FAILED sau khi kỳ thi đã
 * RESULTS_PUBLISHED. Chỉ áp dụng cho FINAL - không đổi lại PASSED/FAILED đã chốt (dù tự động
 * hay thủ công), tránh việc chốt đi chốt lại nhiều lần.
 */
@Service
public class DecideExamCandidateResultOutcomeUseCase
        implements IUseCase<DecideExamCandidateResultOutcomeCommand, UUID> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final ExamRepository examRepository;
    private final ExamSessionModerationAccessService moderationAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public DecideExamCandidateResultOutcomeUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionRepository examSessionRepository,
            ExamCandidateRepository examCandidateRepository,
            ExamRepository examRepository,
            ExamSessionModerationAccessService moderationAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionRepository = examSessionRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.examRepository = examRepository;
        this.moderationAccessService = moderationAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(DecideExamCandidateResultOutcomeCommand input) {
        var result = examCandidateResultRepository.findById(input.candidateResultId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kết quả bài thi"));
        var session = examSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên thi của kết quả này"));
        var candidate = examCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thí sinh của kết quả này"));
        var exam = examRepository.findById(session.getExamId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ thi của kết quả này"));

        moderationAccessService.authorize(exam, candidate);
        if (exam.getStatus() != ExamStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể chốt đậu/rớt sau khi kỳ thi đã công bố kết quả");
        }
        if (result.getStatus() != ExamCandidateResultStatus.FINAL) {
            throw new IllegalStateException("Chỉ có thể chốt đậu/rớt cho kết quả đang ở trạng thái FINAL");
        }
        if (input.decision() != ExamCandidateResultStatus.PASSED
                && input.decision() != ExamCandidateResultStatus.FAILED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ, chỉ chấp nhận PASSED hoặc FAILED");
        }

        var now = Instant.now();
        var actorId = moderationAccessService.getCurrentUserId();
        var before = resultStatusHistoryRecorder.snapshot(result);
        result.setStatus(input.decision());
        result.setUpdatedAt(now);
        result.setUpdatedBy(actorId);
        examCandidateResultRepository.save(result);

        // Hành vi nghiệp vụ giữ nguyên; chỉ thêm dấu vết và thông báo — đây là mốc
        // cuối cùng của bài, mà trước đây học sinh không được báo gì.
        resultStatusHistoryRecorder.record(
            before, result, ResultStatusChangeSource.EXAM_PUBLISH, actorId, null);
        var payload = new ExamResultOutcomeDecidedPayloadV1(
            result.getId(), candidate.getStudentId(), exam.getName(),
            input.decision().name(), result.getTotalScore(),
            result.getSessionId(), exam.getKind());
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.EXAM_CANDIDATE_RESULT,
            result.getId(),
            EventTypeConstant.EXAM_RESULT_OUTCOME_DECIDED,
            jsonSerializationPort.toJson(payload),
            now
        ));
        return result.getId();
    }
}
