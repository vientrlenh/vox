package com.sep.vox.application.port.input.usecase.assessmentsession;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.assessment.AssessmentResultOutcomeDecidedPayload;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DecideAssessmentCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentRepository;
import com.sep.vox.domain.repository.assessment.AssessmentSessionRepository;
import com.sep.vox.domain.shared.AggregateTypeConstant;
import com.sep.vox.domain.shared.EventTypeConstant;

/**
 * G.3 addendum: khi assessmentPolicy không có passingScore, finalizeForPublish chốt kết quả
 * ở FINAL thay vì tự suy ra PASSED/FAILED (không có ngưỡng để so sánh). Use case này cho phép
 * nhà trường tự quyết định thủ công FINAL -> PASSED hoặc FINAL -> FAILED sau khi kỳ thi đã
 * RESULTS_PUBLISHED. Chỉ áp dụng cho FINAL - không đổi lại PASSED/FAILED đã chốt (dù tự động
 * hay thủ công), tránh việc chốt đi chốt lại nhiều lần.
 */
@Service
public class DecideAssessmentCandidateResultOutcomeUseCase
        implements IUseCase<DecideAssessmentCandidateResultOutcomeCommand, UUID> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionModerationAccessService moderationAccessService;
    private final ResultStatusHistoryRecorder resultStatusHistoryRecorder;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public DecideAssessmentCandidateResultOutcomeUseCase(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentSessionRepository assessmentSessionRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            AssessmentRepository assessmentRepository,
            AssessmentSessionModerationAccessService moderationAccessService,
            ResultStatusHistoryRecorder resultStatusHistoryRecorder,
            OutboxRepository outboxRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentCandidateRepository = assessmentCandidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.moderationAccessService = moderationAccessService;
        this.resultStatusHistoryRecorder = resultStatusHistoryRecorder;
        this.outboxRepository = outboxRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public UUID execute(DecideAssessmentCandidateResultOutcomeCommand input) {
        var result = assessmentCandidateResultRepository.findById(input.candidateResultId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi"));
        var session = assessmentSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi của kết quả này"));
        var candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của kết quả này"));
        var assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của kết quả này"));

        moderationAccessService.authorize(assessment, candidate);
        if (assessment.getStatus() != AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalStateException("Chỉ có thể chốt đậu/rớt sau khi kỳ thi đã công bố kết quả");
        }
        if (result.getStatus() != AssessmentCandidateResultStatus.FINAL) {
            throw new IllegalStateException("Chỉ có thể chốt đậu/rớt cho kết quả đang ở trạng thái FINAL");
        }
        if (input.decision() != AssessmentCandidateResultStatus.PASSED
                && input.decision() != AssessmentCandidateResultStatus.FAILED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ, chỉ chấp nhận PASSED hoặc FAILED");
        }

        var now = Instant.now();
        var actorId = moderationAccessService.getCurrentUserId();
        var before = resultStatusHistoryRecorder.snapshot(result);
        result.setStatus(input.decision());
        result.setUpdatedAt(now);
        result.setUpdatedBy(actorId);
        assessmentCandidateResultRepository.save(result);

        // Hành vi nghiệp vụ giữ nguyên; chỉ thêm dấu vết và thông báo — đây là mốc
        // cuối cùng của bài, mà trước đây học sinh không được báo gì.
        resultStatusHistoryRecorder.record(
            before, result, AssessmentResultStatusChangeSource.ASSESSMENT_PUBLISH, actorId, null);
        var payload = new AssessmentResultOutcomeDecidedPayload(
            result.getId(), candidate.getStudentId(), assessment.getName(),
            input.decision().name(), result.getTotalScore(),
            result.getSessionId(), assessment.getKind());
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
            result.getId(),
            EventTypeConstant.ASSESSMENT_RESULT_OUTCOME_DECIDED,
            jsonSerializationPort.toJson(payload),
            now
        ));
        return result.getId();
    }
}
