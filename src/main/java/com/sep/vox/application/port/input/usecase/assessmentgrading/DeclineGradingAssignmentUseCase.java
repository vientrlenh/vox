package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingAssignmentDeclinedPayloadV1;
import com.sep.vox.application.port.input.command.assessmentgrading.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.response.input.assessmentgrading.GradingActionResponse;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;

/**
 * Giáo viên trả lại phân công kèm lý do (quen biết thí sinh, quá tải...).
 *
 * <p>Bài KHÔNG đổi trạng thái — nó chỉ quay về hàng chưa giao. Dòng phân công vẫn
 * được giữ với {@code outcome = DECLINED}: xoá đi thì mất luôn thông tin "người này
 * đã từ chối bài này", mà đó chính là thứ admin cần khi giao lại.
 */
@Service
@RequiredArgsConstructor
public class DeclineGradingAssignmentUseCase
        implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.DECLINED, command.reason());

        AssessmentCandidateResult result = prepared.context().candidateResult();
        AssessmentGradingAssignment assignment = prepared.context().assignment();
        gradingActionSupport.finish(prepared, result);
        publishDeclined(prepared, assignment, result);

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.DECLINED.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            null
        );
    }

    /**
     * The person who needs to know is the admin who assigned, not the student: the attempt is waiting for
     * the coordinator to handle it next.
     */
    private void publishDeclined(
            GradingActionSupport.PreparedAction prepared,
            AssessmentGradingAssignment assignment,
            AssessmentCandidateResult result) {
        GradingAssignmentDeclinedPayloadV1 payload = new GradingAssignmentDeclinedPayloadV1(
            assignment.getId(),
            result.getId(),
            assignment.getTeacherId(),
            assignment.getAssignedBy(),
            prepared.context().assessmentName(),
            prepared.reason(),
            result.getAssessmentId(),
            prepared.context().assessmentKind()
        );
        outboxRepository.save(Outbox.create(
            AggregateTypeConstant.ASSESSMENT_GRADING_ASSIGNMENT,
            assignment.getId(),
            EventTypeConstant.GRADING_ASSIGNMENT_DECLINED,
            jsonSerializationPort.toJson(payload),
            Instant.now()
        ));
    }
}
