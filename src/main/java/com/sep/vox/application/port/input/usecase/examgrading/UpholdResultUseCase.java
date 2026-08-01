package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Giáo viên xác nhận điểm đang có là đúng — không nhập điểm mới.
 *
 * <p>Đây là hành động gộp của bản rework: "admin release bài chờ chấm" và "hậu kiểm
 * rồi giữ nguyên" hoá ra là cùng một quyết định (điểm hiện có đúng), chỉ khác vòng.
 * Vòng {@code INITIAL} thì bài chuyển sang RELEASED; vòng {@code SPOT_CHECK} thì bài
 * vốn đã RELEASED nên không đổi gì và <em>không</em> gửi mail.
 *
 * <p>Không tính lại điểm: không có item nào thay đổi, nên tổng vẫn là hàm của đúng
 * các item cũ. Bất biến {@code total = f(items)} được giữ mà không phải chạm gì.
 */
@Service
public class UpholdResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final ExamSessionRepository examSessionRepository;

    public UpholdResultUseCase(
            GradingActionSupport gradingActionSupport,
            ExamSessionRepository examSessionRepository) {
        this.gradingActionSupport = gradingActionSupport;
        this.examSessionRepository = examSessionRepository;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.UPHELD, command.reason());

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm. Giữ nguyên
        // flagReason để còn tra lại vì sao bài từng bị đánh dấu.
        //
        // Ở REMEDIATION thì KHÔNG gỡ: giữ nguyên ở vòng đó nghĩa là xác nhận vi phạm,
        // gỡ cờ lúc này sẽ nói ngược lại chính quyết định vừa ghi.
        var session = prepared.context().session();
        if (prepared.roundType() != GradingRoundType.REMEDIATION && session.isFlagged()) {
            session.setFlagged(false);
            examSessionRepository.save(session);
        }

        var result = prepared.context().candidateResult();
        gradingActionSupport.finish(prepared, result);

        return new GradingActionResponse(
            command.assignmentId(),
            result.getId(),
            GradingOutcome.UPHELD.name(),
            result.getStatus() == null ? null : result.getStatus().name(),
            result.getTotalScore(),
            null
        );
    }
}
