package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.event.GradingAssignmentDeclinedEvent;
import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.GradingOutcome;

/**
 * Giáo viên trả lại phân công kèm lý do (quen biết thí sinh, quá tải...).
 *
 * <p>Bài KHÔNG đổi trạng thái — nó chỉ quay về hàng chưa giao. Dòng phân công vẫn
 * được giữ với {@code outcome = DECLINED}: xoá đi thì mất luôn thông tin "người này
 * đã từ chối bài này", mà đó chính là thứ admin cần khi giao lại.
 */
@Service
public class DeclineGradingAssignmentUseCase
        implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final EventPublisherPort eventPublisherPort;

    public DeclineGradingAssignmentUseCase(
            GradingActionSupport gradingActionSupport,
            EventPublisherPort eventPublisherPort) {
        this.gradingActionSupport = gradingActionSupport;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.DECLINED, command.reason());

        var result = prepared.context().candidateResult();
        var assignment = prepared.context().assignment();
        gradingActionSupport.finish(prepared, result);

        // Người cần biết là admin đã giao, không phải học sinh: bài đang nằm chờ
        // người điều phối xử lý tiếp.
        eventPublisherPort.publish(new GradingAssignmentDeclinedEvent(
            assignment.getId(),
            result.getId(),
            assignment.getTeacherId(),
            assignment.getAssignedBy(),
            prepared.context().examName(),
            prepared.reason()
        ));

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.DECLINED.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            null
        );
    }
}
