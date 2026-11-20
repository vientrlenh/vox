package com.sep.vox.application.port.input.usecase.examitemresponse;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.ExamItemResponseRepository;

/**
 * Tra cứu sessionId từ responseId -- dùng ở DLT handler của
 * ExamAttemptEvaluationCompletedConsumer khi payload lỗi không parse được examAttemptId trực
 * tiếp, chỉ còn answerId (responseId) để suy ra session cần đánh dấu GRADING_FAILED. Tách thành
 * use case riêng vì interfaces layer (Kafka consumer) không được gọi thẳng domain repository.
 */
@Service
public class ResolveExamSessionIdByResponseIdUseCase implements IUseCase<UUID, UUID> {

    private final ExamItemResponseRepository examItemResponseRepository;

    public ResolveExamSessionIdByResponseIdUseCase(ExamItemResponseRepository examItemResponseRepository) {
        this.examItemResponseRepository = examItemResponseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID execute(UUID responseId) {
        if (responseId == null) {
            return null;
        }
        return examItemResponseRepository.findById(responseId)
            .map(response -> response.getSessionId())
            .orElse(null);
    }
}
