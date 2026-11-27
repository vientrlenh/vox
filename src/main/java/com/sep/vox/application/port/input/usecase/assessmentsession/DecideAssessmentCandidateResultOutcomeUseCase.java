package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentResultOutcomeDecidedPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.assessmentsession.DecideAssessmentCandidateResultOutcomeCommand;
import com.sep.vox.application.port.input.service.AssessmentSessionModerationAccessService;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultStatusChangeSource;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * G.3 addendum: khi assessmentPolicy không có passingScore, finalizeForPublish chốt kết quả
 * ở FINAL thay vì tự suy ra PASSED/FAILED (không có ngưỡng để so sánh). Use case này cho phép
 * nhà trường tự quyết định thủ công FINAL -> PASSED hoặc FINAL -> FAILED sau khi kỳ thi đã
 * RESULTS_PUBLISHED. Chỉ áp dụng cho FINAL - không đổi lại PASSED/FAILED đã chốt (dù tự động
 * hay thủ công), tránh việc chốt đi chốt lại nhiều lần.
 */
@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public UUID execute(DecideAssessmentCandidateResultOutcomeCommand input) {
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findById(input.candidateResultId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả bài thi"));
        AssessmentSession session = assessmentSessionRepository.findById(result.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên thi của kết quả này"));
        AssessmentCandidate candidate = assessmentCandidateRepository.findById(session.getCandidateId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thí sinh của kết quả này"));
        Assessment assessment = assessmentRepository.findById(session.getAssessmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ thi của kết quả này"));

        moderationAccessService.authorize(assessment, candidate);
        requireDecidable(input, assessment, result);

        Instant now = Instant.now();
        UUID actorId = moderationAccessService.getCurrentUserId();
        ResultStatusHistoryRecorder.Snapshot before = resultStatusHistoryRecorder.snapshot(result);
        result.setStatus(input.decision());
        assessmentCandidateResultRepository.save(result);

        resultStatusHistoryRecorder.record(
            before, result, ResultStatusChangeSource.ASSESSMENT_PUBLISH, actorId, null);
        saveOutcomeDecidedOutbox(input, assessment, candidate, result, now);
        return result.getId();
    }

    private void requireDecidable(
        DecideAssessmentCandidateResultOutcomeCommand input, Assessment assessment, AssessmentCandidateResult result) {
        if (assessment.getStatus() != AssessmentStatus.RESULTS_PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể chốt đậu/rớt sau khi kỳ thi đã công bố kết quả");
        }
        if (result.getStatus() != AssessmentCandidateResultStatus.FINAL) {
            throw new IllegalArgumentException("Chỉ có thể chốt đậu/rớt cho kết quả đang ở trạng thái FINAL");
        }
        if (input.decision() != AssessmentCandidateResultStatus.PASSED
                && input.decision() != AssessmentCandidateResultStatus.FAILED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ, chỉ chấp nhận PASSED hoặc FAILED");
        }
    }

    /**
     * The business behaviour is unchanged; only the trail and the notification are added -- this is the final
     * milestone of the attempt, and the student used to be told nothing about it.
     */
    private void saveOutcomeDecidedOutbox(
        DecideAssessmentCandidateResultOutcomeCommand input,
        Assessment assessment,
        AssessmentCandidate candidate,
        AssessmentCandidateResult result,
        Instant now) {
        AssessmentResultOutcomeDecidedPayloadV1 payload = new AssessmentResultOutcomeDecidedPayloadV1(
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
    }
}
