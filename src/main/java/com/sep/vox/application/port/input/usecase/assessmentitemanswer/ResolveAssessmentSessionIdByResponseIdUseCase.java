package com.sep.vox.application.port.input.usecase.assessmentitemanswer;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;

/**
 * Tra cứu sessionId từ responseId -- dùng ở DLT handler của
 * AssessmentAttemptEvaluationCompletedConsumer khi payload lỗi không parse được assessmentAttemptId trực
 * tiếp, chỉ còn answerId (responseId) để suy ra session cần đánh dấu GRADING_FAILED. Tách thành
 * use case riêng vì interfaces layer (Kafka consumer) không được gọi thẳng domain repository.
 */
@Service
public class ResolveAssessmentSessionIdByResponseIdUseCase implements IUseCase<UUID, UUID> {

    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;

    public ResolveAssessmentSessionIdByResponseIdUseCase(AssessmentItemAnswerRepository assessmentItemAnswerRepository) {
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID execute(UUID responseId) {
        if (responseId == null) {
            return null;
        }
        return assessmentItemAnswerRepository.findById(responseId)
            .map(response -> response.getSessionId())
            .orElse(null);
    }
}
