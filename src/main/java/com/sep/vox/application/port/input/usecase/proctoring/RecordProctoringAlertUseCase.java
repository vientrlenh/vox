package com.sep.vox.application.port.input.usecase.proctoring;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.command.proctoring.RecordProctoringAlertCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.assessment.AssessmentProctoringAlert;
import com.sep.vox.domain.repository.AssessmentProctoringAlertRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * Ghi một cảnh báo giám sát vào sổ.
 *
 * <p>Cố ý KHÔNG đánh {@code @Transactional}: chống trùng dựa vào ràng buộc unique trên
 * {@code event_id}, và adapter phải bắt được vi phạm đó rồi đi tiếp. Trong một transaction do use
 * case mở, lần ghi hỏng sẽ đánh dấu transaction rollback-only và mọi thứ sau đó đều vô nghĩa dù đã
 * bắt exception. Để mỗi lần ghi tự lo transaction của nó thì việc nuốt lỗi mới thật sự nuốt được.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordProctoringAlertUseCase implements IUseCase<RecordProctoringAlertCommand, Boolean> {

    private final AssessmentProctoringAlertRepository assessmentProctoringAlertRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Override
    public Boolean execute(RecordProctoringAlertCommand input) {
        // Phiên thi không tồn tại thì BỎ QUA chứ không ném lỗi. Khác hẳn đường recording, nơi một
        // sessionId lạ là dấu hiệu hỏng thật và đáng dừng lại: ở đây nguồn phát bao gồm cả những
        // phiên WebRTC mà AI service tự sinh id, cộng dữ liệu tồn từ trước bản vá định danh. Ném lỗi
        // sẽ đẩy chúng vào DLT rồi retry mãi, làm tắc luôn những cảnh báo hợp lệ đứng sau trong cùng
        // partition -- đánh đổi tệ hại cho một bản ghi mà ta vốn không dùng được.
        if (!assessmentSessionRepository.findById(input.assessmentSessionId()).isPresent()) {
            log.warn(
                "Bỏ qua cảnh báo giám sát vì không tìm thấy phiên thi: eventId={} assessmentSessionId={} alertType={}",
                input.eventId(), input.assessmentSessionId(), input.alertType()
            );
            return false;
        }

        AssessmentProctoringAlert alert = AssessmentProctoringAlert.raised(
                input.eventId(), input.assessmentSessionId(), input.candidateId(), input.streamId(), input.streamType(),
                input.alertType(), input.level(), input.source(), input.detail(), input.confidence(),
                input.sequenceNo(), input.capturedAt(), input.raisedAt(), Instant.now());

        return assessmentProctoringAlertRepository.saveIfAbsent(alert);
    }
}
