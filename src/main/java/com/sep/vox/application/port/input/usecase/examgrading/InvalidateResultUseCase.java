package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.ExamResultInvalidatedEvent;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.GradingOutcome;

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
public class InvalidateResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final EventPublisherPort eventPublisherPort;

    public InvalidateResultUseCase(
            GradingActionSupport gradingActionSupport,
            EventPublisherPort eventPublisherPort) {
        this.gradingActionSupport = gradingActionSupport;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.INVALIDATED, command.reason());

        var result = prepared.context().candidateResult();
        gradingActionSupport.finish(prepared, result);

        var studentId = gradingActionSupport.resolveStudentId(result);
        if (studentId != null) {
            eventPublisherPort.publish(new ExamResultInvalidatedEvent(
                result.getId(), studentId, prepared.context().examName(), prepared.reason()));
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
