package com.sep.vox.application.port.input.usecase.assessmentgrading;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.AssessmentResultInvalidatedPayloadV1;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
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
 * Giáo viên kết luận bài là vi phạm thật -> result INVALID, không nhập điểm.
 *
 * <p>Hai thay đổi so với bản cũ:
 * <ul>
 *   <li><strong>Lý do bắt buộc</strong> (ép ở {@code GradingRoundPolicy}). Bản cũ để
 *       trống được, nên tranh chấp về sau không giải trình được.
 *   <li><strong>Bỏ ràng buộc {@code session.isFlagged()}</strong>. Gian lận thường lộ
 *       ra khi ĐỌC BÀI chứ không chỉ khi giám thị kịp bấm cờ lúc thi; bắt phải có cờ
 *       trước là buộc giáo viên hoặc bỏ qua vi phạm, hoặc đi đường vòng.
 * </ul>
 *
 * <p>Quyền đến từ <em>dòng phân công</em> chứ không từ vai trò coi thi: giáo viên
 * được giao bài nào thì quyết được đúng bài đó.
 */
@Service
@RequiredArgsConstructor
public class InvalidateResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final OutboxRepository outboxRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        GradingActionSupport.PreparedAction prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.INVALIDATED, command.reason());

        AssessmentCandidateResult result = prepared.context().candidateResult();
        gradingActionSupport.finish(prepared, result);

        UUID studentId = gradingActionSupport.resolveStudentId(result);
        if (studentId != null) {
            AssessmentResultInvalidatedPayloadV1 payload = new AssessmentResultInvalidatedPayloadV1(
                result.getId(), studentId, prepared.context().assessmentName(), prepared.reason(),
                result.getSessionId(), prepared.context().assessmentKind());
            outboxRepository.save(Outbox.create(
                AggregateTypeConstant.ASSESSMENT_CANDIDATE_RESULT,
                result.getId(),
                EventTypeConstant.ASSESSMENT_RESULT_INVALIDATED,
                jsonSerializationPort.toJson(payload),
                Instant.now()
            ));
        }

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.INVALIDATED.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            null
        );
    }
}
